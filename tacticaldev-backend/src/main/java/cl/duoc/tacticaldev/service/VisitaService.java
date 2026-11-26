package cl.duoc.tacticaldev.service;

import cl.duoc.tacticaldev.dto.ValidarRequest;
import cl.duoc.tacticaldev.dto.VisitaRequest;
import cl.duoc.tacticaldev.dto.VisitaResponse;
import cl.duoc.tacticaldev.exception.ApiException;
import cl.duoc.tacticaldev.model.*;
import cl.duoc.tacticaldev.repository.VisitaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class VisitaService {

    private final VisitaRepository repo;
    private final SolicitudService solicitudService;
    private final EquipoService equipoService;
    private final VerificacionService verificacion;

    public VisitaService(VisitaRepository repo, SolicitudService solicitudService,
                         EquipoService equipoService, VerificacionService verificacion) {
        this.repo = repo;
        this.solicitudService = solicitudService;
        this.equipoService = equipoService;
        this.verificacion = verificacion;
    }

    public List<VisitaResponse> listar(Long operarioId, Long solicitudId, EstadoValidacion estado) {
        List<Visita> lista;
        if (operarioId != null) lista = repo.findByOperarioIdOrderByFechaRegistroDesc(operarioId);
        else if (solicitudId != null) lista = repo.findBySolicitudIdOrderByFechaRegistroDesc(solicitudId);
        else if (estado != null) lista = repo.findByEstadoValidacionOrderByFechaRegistroDesc(estado);
        else lista = repo.findAllByOrderByFechaRegistroDesc();
        return lista.stream().map(v -> VisitaResponse.from(v, false)).toList();
    }

    public VisitaResponse obtener(Long id) {
        return VisitaResponse.from(buscar(id), true);
    }

    /**
     * Registra la entrega/visita. El servidor vuelve a calcular la distancia entre
     * el GPS de la foto y el punto de venta: la app no puede "decir" que está verificada.
     */
    @Transactional
    public VisitaResponse registrar(VisitaRequest req) {
        // Idempotencia: si la app reintenta la sincronización no se duplica
        Optional<Visita> existente = repo.findByIdLocal(req.idLocal());
        if (existente.isPresent()) return VisitaResponse.from(existente.get(), false);

        Solicitud s = solicitudService.buscar(req.solicitudId());
        if (s.getEstado() != EstadoSolicitud.ASIGNADA) {
            throw new ApiException(HttpStatus.CONFLICT, "La solicitud no está pendiente de visita (estado " + s.getEstado() + ")");
        }
        if (!req.operarioId().equals(s.getOperarioId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Esta solicitud está asignada a otro operario");
        }
        LocalDateTime ahora = LocalDateTime.now();
        String errorFecha = verificacion.validarFechaCaptura(req.fechaCaptura(), ahora);
        if (errorFecha != null) throw new ApiException(HttpStatus.BAD_REQUEST, errorFecha);

        PuntoVenta pv = s.getPuntoVenta();
        VerificacionService.Resultado r = verificacion.verificar(req.latitud(), req.longitud(),
                req.precisionMetros(), req.ubicacionSimulada(), pv.getLatitud(), pv.getLongitud(), pv.getRadioMetros());

        Visita v = new Visita();
        v.setIdLocal(req.idLocal());
        v.setSolicitud(s);
        v.setOperarioId(req.operarioId());
        v.setOperarioNombre(s.getOperarioNombre());
        v.setEstadoEquipo(req.estadoEquipo());
        v.setObservaciones(req.observaciones());
        v.setFotoBase64(req.fotoBase64());
        v.setLatitud(req.latitud());
        v.setLongitud(req.longitud());
        v.setPrecisionMetros(req.precisionMetros());
        v.setUbicacionSimulada(req.ubicacionSimulada());
        v.setFechaCaptura(req.fechaCaptura());
        v.setFechaRegistro(ahora);
        v.setTemperaturaAmbiente(req.temperaturaAmbiente());
        v.setDistanciaMetros(r.distanciaMetros());
        v.setUbicacionVerificada(r.verificada());
        v.setMotivoVerificacion(r.motivo());
        v.setEstadoValidacion(r.verificada() ? EstadoValidacion.PENDIENTE : EstadoValidacion.SOSPECHOSA);

        if (r.verificada()) {
            s.setEstado(EstadoSolicitud.COMPLETADA);
            solicitudService.guardar(s);
        }
        return VisitaResponse.from(repo.save(v), false);
    }

    /** El supervisor aprueba o rechaza. Al aprobar se actualiza el equipo. */
    @Transactional
    public VisitaResponse validar(Long id, ValidarRequest req) {
        Visita v = buscar(id);
        if (v.getEstadoValidacion() != EstadoValidacion.PENDIENTE && v.getEstadoValidacion() != EstadoValidacion.SOSPECHOSA) {
            throw new ApiException(HttpStatus.CONFLICT, "La visita ya fue revisada");
        }
        boolean sinComentario = req.comentario() == null || req.comentario().isBlank();
        Solicitud s = v.getSolicitud();

        if (req.aprobada()) {
            if (s.getEstado() == EstadoSolicitud.VALIDADA) {
                throw new ApiException(HttpStatus.CONFLICT, "La solicitud ya fue cerrada con otra visita");
            }
            if (v.getEstadoValidacion() == EstadoValidacion.SOSPECHOSA && sinComentario) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Para aprobar una visita sospechosa debe justificar en el comentario");
            }
            v.setEstadoValidacion(EstadoValidacion.VALIDADA);
            s.setEstado(EstadoSolicitud.VALIDADA);
            s.setFechaCierre(LocalDateTime.now());
            aplicarEfectoEnEquipo(s, v);
        } else {
            if (sinComentario) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Indique el motivo del rechazo");
            }
            v.setEstadoValidacion(EstadoValidacion.RECHAZADA);
            // Si esta visita era la que completó la solicitud, debe volver a realizarse
            if (s.getEstado() == EstadoSolicitud.COMPLETADA && v.isUbicacionVerificada()) {
                s.setEstado(EstadoSolicitud.ASIGNADA);
            }
        }
        v.setComentarioSupervisor(req.comentario());
        v.setFechaValidacion(LocalDateTime.now());
        solicitudService.guardar(s);
        return VisitaResponse.from(repo.save(v), false);
    }

    private void aplicarEfectoEnEquipo(Solicitud s, Visita v) {
        Equipo e = s.getEquipo();
        if (e == null) return;
        switch (s.getTipo()) {
            case EQUIPO_NUEVO -> {
                e.setPuntoVenta(s.getPuntoVenta());
                e.setEstado(v.getEstadoEquipo());
            }
            case RETIRO -> {
                e.setPuntoVenta(null);
                e.setEstado(EstadoEquipo.RETIRADO);
            }
            case REPARACION, REVISION -> e.setEstado(v.getEstadoEquipo());
        }
        equipoService.guardar(e);
    }

    private Visita buscar(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Visita " + id + " no encontrada"));
    }
}

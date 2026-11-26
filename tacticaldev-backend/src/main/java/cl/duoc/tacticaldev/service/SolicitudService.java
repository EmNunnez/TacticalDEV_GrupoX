package cl.duoc.tacticaldev.service;

import cl.duoc.tacticaldev.dto.AsignarRequest;
import cl.duoc.tacticaldev.dto.SolicitudRequest;
import cl.duoc.tacticaldev.dto.SolicitudResponse;
import cl.duoc.tacticaldev.dto.SolicitudUpdateRequest;
import cl.duoc.tacticaldev.exception.ApiException;
import cl.duoc.tacticaldev.model.*;
import cl.duoc.tacticaldev.repository.SolicitudRepository;
import cl.duoc.tacticaldev.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class SolicitudService {

    /** Estados en que una solicitud sigue "abierta". */
    static final Set<EstadoSolicitud> ABIERTAS =
            EnumSet.of(EstadoSolicitud.PENDIENTE, EstadoSolicitud.ASIGNADA, EstadoSolicitud.COMPLETADA);

    private final SolicitudRepository repo;
    private final PuntoVentaService puntoVentaService;
    private final EquipoService equipoService;
    private final UsuarioRepository usuarioRepo;
    private final VerificacionService verificacion;

    public SolicitudService(SolicitudRepository repo, PuntoVentaService puntoVentaService,
                            EquipoService equipoService, UsuarioRepository usuarioRepo,
                            VerificacionService verificacion) {
        this.repo = repo;
        this.puntoVentaService = puntoVentaService;
        this.equipoService = equipoService;
        this.usuarioRepo = usuarioRepo;
        this.verificacion = verificacion;
    }

    public List<SolicitudResponse> listar(Long clienteId, Long operarioId, EstadoSolicitud estado) {
        List<Solicitud> lista;
        if (clienteId != null) lista = repo.findByClienteIdOrderByFechaCreacionDesc(clienteId);
        else if (operarioId != null) lista = repo.findByOperarioIdOrderByFechaCreacionDesc(operarioId);
        else if (estado != null) lista = repo.findByEstadoOrderByFechaCreacionDesc(estado);
        else lista = repo.findAllByOrderByFechaCreacionDesc();
        if (estado != null && (clienteId != null || operarioId != null)) {
            lista = lista.stream().filter(s -> s.getEstado() == estado).toList();
        }
        return lista.stream().map(s -> SolicitudResponse.from(s, false)).toList();
    }

    public SolicitudResponse obtener(Long id) {
        return SolicitudResponse.from(buscar(id), true);
    }

    @Transactional
    public SolicitudResponse crear(SolicitudRequest req) {
        PuntoVenta pv = puntoVentaService.buscar(req.puntoVentaId());
        if (!pv.getClienteId().equals(req.clienteId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El punto de venta no pertenece a este cliente");
        }

        Solicitud s = new Solicitud();
        s.setTipo(req.tipo());
        s.setDescripcion(req.descripcion().trim());
        s.setClienteId(req.clienteId());
        s.setClienteNombre(usuarioRepo.findById(req.clienteId()).map(Usuario::getNombre).orElse("Cliente #" + req.clienteId()));
        s.setPuntoVenta(pv);
        s.setFotoClienteBase64(req.fotoClienteBase64());

        if (req.tipo() == TipoSolicitud.EQUIPO_NUEVO) {
            if (req.tipoEquipoSolicitado() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Indique el tipo de equipo que necesita");
            }
            s.setTipoEquipoSolicitado(req.tipoEquipoSolicitado());
        } else {
            if (req.equipoId() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Debe seleccionar el equipo");
            }
            Equipo e = equipoService.buscar(req.equipoId());
            if (e.getPuntoVenta() == null || !e.getPuntoVenta().getId().equals(pv.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El equipo no está instalado en ese punto de venta");
            }
            if (repo.existsByEquipoIdAndEstadoIn(e.getId(), ABIERTAS)) {
                throw new ApiException(HttpStatus.CONFLICT, "Ya existe una solicitud abierta para el equipo " + e.getCodigo());
            }
            if (req.tipo() == TipoSolicitud.REPARACION) {
                // El cliente reporta la falla: queda visible de inmediato para el supervisor
                e.setEstado(EstadoEquipo.CON_FALLA);
                equipoService.guardar(e);
            }
            s.setEquipo(e);
        }
        return SolicitudResponse.from(repo.save(s), true);
    }

    @Transactional
    public SolicitudResponse actualizar(Long id, SolicitudUpdateRequest req) {
        Solicitud s = buscar(id);
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo se pueden editar solicitudes PENDIENTES");
        }
        s.setDescripcion(req.descripcion().trim());
        return SolicitudResponse.from(repo.save(s), true);
    }

    @Transactional
    public SolicitudResponse asignar(Long id, AsignarRequest req) {
        Solicitud s = buscar(id);
        if (s.getEstado() != EstadoSolicitud.PENDIENTE && s.getEstado() != EstadoSolicitud.ASIGNADA) {
            throw new ApiException(HttpStatus.CONFLICT, "La solicitud ya fue atendida y no se puede reasignar");
        }

        Usuario op = usuarioRepo.findById(req.operarioId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario " + req.operarioId() + " no existe"));
        if (op.getRol() != Rol.OPERARIO || !op.isActivo()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El usuario seleccionado no es un operario activo");
        }
        String tipoOperario = op.getTipoOperario() == null ? null : op.getTipoOperario().name();
        if (!verificacion.operarioCompatible(s.getTipo(), tipoOperario)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Un " + tipoOperario
                    + " no puede atender solicitudes de " + s.getTipo());
        }

        if (s.getTipo() == TipoSolicitud.EQUIPO_NUEVO) {
            if (req.equipoId() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Seleccione el equipo de bodega que se instalará");
            }
            Equipo e = equipoService.buscar(req.equipoId());
            boolean mismoEquipo = s.getEquipo() != null && s.getEquipo().getId().equals(e.getId());
            if (e.getPuntoVenta() != null || e.getEstado() != EstadoEquipo.OPERATIVO) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El equipo debe estar en bodega y OPERATIVO");
            }
            if (e.getTipo() != s.getTipoEquipoSolicitado()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "El cliente pidió " + s.getTipoEquipoSolicitado());
            }
            if (!mismoEquipo && repo.existsByEquipoIdAndEstadoIn(e.getId(), ABIERTAS)) {
                throw new ApiException(HttpStatus.CONFLICT, "El equipo ya está reservado para otra solicitud");
            }
            s.setEquipo(e);
        }

        s.setOperarioId(op.getId());
        s.setOperarioNombre(op.getNombre());
        s.setFechaAsignacion(LocalDateTime.now());
        s.setEstado(EstadoSolicitud.ASIGNADA);
        return SolicitudResponse.from(repo.save(s), false);
    }

    @Transactional
    public void eliminar(Long id) {
        Solicitud s = buscar(id);
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ApiException(HttpStatus.CONFLICT, "Solo se pueden cancelar solicitudes PENDIENTES");
        }
        repo.delete(s);
    }

    public Solicitud buscar(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Solicitud " + id + " no encontrada"));
    }

    public Solicitud guardar(Solicitud s) {
        return repo.save(s);
    }
}

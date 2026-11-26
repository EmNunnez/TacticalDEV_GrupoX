package cl.duoc.tacticaldev.service;

import cl.duoc.tacticaldev.dto.EquipoRequest;
import cl.duoc.tacticaldev.dto.EquipoResponse;
import cl.duoc.tacticaldev.dto.VisitaResponse;
import cl.duoc.tacticaldev.exception.ApiException;
import cl.duoc.tacticaldev.model.Equipo;
import cl.duoc.tacticaldev.model.EstadoEquipo;
import cl.duoc.tacticaldev.repository.EquipoRepository;
import cl.duoc.tacticaldev.repository.SolicitudRepository;
import cl.duoc.tacticaldev.repository.VisitaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EquipoService {

    private final EquipoRepository repo;
    private final PuntoVentaService puntoVentaService;
    private final SolicitudRepository solicitudRepo;
    private final VisitaRepository visitaRepo;

    public EquipoService(EquipoRepository repo, PuntoVentaService puntoVentaService,
                         SolicitudRepository solicitudRepo, VisitaRepository visitaRepo) {
        this.repo = repo;
        this.puntoVentaService = puntoVentaService;
        this.solicitudRepo = solicitudRepo;
        this.visitaRepo = visitaRepo;
    }

    /**
     * @param puntoVentaId filtra por punto de venta
     * @param enBodega     true = solo equipos sin punto de venta (stock)
     */
    public List<EquipoResponse> listar(Long puntoVentaId, Boolean enBodega) {
        List<Equipo> lista;
        if (puntoVentaId != null) lista = repo.findByPuntoVentaIdOrderByCodigoAsc(puntoVentaId);
        else if (Boolean.TRUE.equals(enBodega)) lista = repo.findByPuntoVentaIsNullOrderByCodigoAsc();
        else lista = repo.findAll();
        return lista.stream().map(EquipoResponse::from).toList();
    }

    public EquipoResponse obtener(Long id) {
        return EquipoResponse.from(buscar(id));
    }

    /** Historial único y consultable del activo (señal de éxito del caso). */
    public List<VisitaResponse> historial(Long id) {
        buscar(id);
        return visitaRepo.findBySolicitudEquipoIdOrderByFechaRegistroDesc(id).stream()
                .map(v -> VisitaResponse.from(v, false)).toList();
    }

    @Transactional
    public EquipoResponse crear(EquipoRequest req) {
        if (repo.existsByCodigoIgnoreCase(req.codigo())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un equipo con código " + req.codigo().toUpperCase());
        }
        Equipo e = new Equipo();
        aplicar(e, req);
        return EquipoResponse.from(repo.save(e));
    }

    @Transactional
    public EquipoResponse actualizar(Long id, EquipoRequest req) {
        Equipo e = buscar(id);
        if (!e.getCodigo().equalsIgnoreCase(req.codigo()) && repo.existsByCodigoIgnoreCase(req.codigo())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un equipo con código " + req.codigo().toUpperCase());
        }
        aplicar(e, req);
        return EquipoResponse.from(repo.save(e));
    }

    @Transactional
    public void eliminar(Long id) {
        Equipo e = buscar(id);
        if (solicitudRepo.existsByEquipoId(id)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "No se puede eliminar: el equipo tiene historial. Cámbielo a estado RETIRADO");
        }
        repo.delete(e);
    }

    public Equipo buscar(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Equipo " + id + " no encontrado"));
    }

    public Equipo guardar(Equipo e) {
        return repo.save(e);
    }

    private void aplicar(Equipo e, EquipoRequest req) {
        e.setCodigo(req.codigo().trim().toUpperCase());
        e.setTipo(req.tipo());
        e.setMarca(req.marca().trim());
        e.setModelo(req.modelo().trim());
        e.setEstado(req.estado() == null ? EstadoEquipo.OPERATIVO : req.estado());
        e.setPuntoVenta(req.puntoVentaId() == null ? null : puntoVentaService.buscar(req.puntoVentaId()));
    }
}

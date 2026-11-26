package cl.duoc.tacticaldev.service;

import cl.duoc.tacticaldev.dto.PuntoVentaRequest;
import cl.duoc.tacticaldev.dto.PuntoVentaResponse;
import cl.duoc.tacticaldev.exception.ApiException;
import cl.duoc.tacticaldev.model.PuntoVenta;
import cl.duoc.tacticaldev.repository.EquipoRepository;
import cl.duoc.tacticaldev.repository.PuntoVentaRepository;
import cl.duoc.tacticaldev.repository.SolicitudRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PuntoVentaService {

    private final PuntoVentaRepository repo;
    private final EquipoRepository equipoRepo;
    private final SolicitudRepository solicitudRepo;

    public PuntoVentaService(PuntoVentaRepository repo, EquipoRepository equipoRepo, SolicitudRepository solicitudRepo) {
        this.repo = repo;
        this.equipoRepo = equipoRepo;
        this.solicitudRepo = solicitudRepo;
    }

    public List<PuntoVentaResponse> listar(Long clienteId) {
        List<PuntoVenta> lista = clienteId == null ? repo.findAll() : repo.findByClienteIdOrderByNombreAsc(clienteId);
        return lista.stream().map(PuntoVentaResponse::from).toList();
    }

    public PuntoVentaResponse obtener(Long id) {
        return PuntoVentaResponse.from(buscar(id));
    }

    @Transactional
    public PuntoVentaResponse crear(PuntoVentaRequest req) {
        PuntoVenta p = new PuntoVenta();
        aplicar(p, req);
        return PuntoVentaResponse.from(repo.save(p));
    }

    @Transactional
    public PuntoVentaResponse actualizar(Long id, PuntoVentaRequest req) {
        PuntoVenta p = buscar(id);
        aplicar(p, req);
        return PuntoVentaResponse.from(repo.save(p));
    }

    @Transactional
    public void eliminar(Long id) {
        PuntoVenta p = buscar(id);
        if (equipoRepo.existsByPuntoVentaId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede eliminar: el punto de venta tiene equipos instalados");
        }
        if (solicitudRepo.existsByPuntoVentaId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede eliminar: el punto de venta tiene historial de solicitudes");
        }
        repo.delete(p);
    }

    public PuntoVenta buscar(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Punto de venta " + id + " no encontrado"));
    }

    private void aplicar(PuntoVenta p, PuntoVentaRequest req) {
        p.setNombre(req.nombre().trim());
        p.setDireccion(req.direccion().trim());
        p.setComuna(req.comuna().trim());
        p.setLatitud(req.latitud());
        p.setLongitud(req.longitud());
        p.setRadioMetros(req.radioMetros());
        p.setClienteId(req.clienteId());
    }
}

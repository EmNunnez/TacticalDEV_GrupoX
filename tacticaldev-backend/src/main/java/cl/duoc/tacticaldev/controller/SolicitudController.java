package cl.duoc.tacticaldev.controller;

import cl.duoc.tacticaldev.dto.AsignarRequest;
import cl.duoc.tacticaldev.dto.SolicitudRequest;
import cl.duoc.tacticaldev.dto.SolicitudResponse;
import cl.duoc.tacticaldev.dto.SolicitudUpdateRequest;
import cl.duoc.tacticaldev.model.EstadoSolicitud;
import cl.duoc.tacticaldev.service.SolicitudService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService service;

    public SolicitudController(SolicitudService service) {
        this.service = service;
    }

    @GetMapping
    public List<SolicitudResponse> listar(@RequestParam(required = false) Long clienteId,
                                          @RequestParam(required = false) Long operarioId,
                                          @RequestParam(required = false) EstadoSolicitud estado) {
        return service.listar(clienteId, operarioId, estado);
    }

    @GetMapping("/{id}")
    public SolicitudResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SolicitudResponse crear(@Valid @RequestBody SolicitudRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    public SolicitudResponse actualizar(@PathVariable Long id, @Valid @RequestBody SolicitudUpdateRequest req) {
        return service.actualizar(id, req);
    }

    @PutMapping("/{id}/asignar")
    public SolicitudResponse asignar(@PathVariable Long id, @Valid @RequestBody AsignarRequest req) {
        return service.asignar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}

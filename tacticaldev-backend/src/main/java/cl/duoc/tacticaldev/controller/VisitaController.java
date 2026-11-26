package cl.duoc.tacticaldev.controller;

import cl.duoc.tacticaldev.dto.ValidarRequest;
import cl.duoc.tacticaldev.dto.VisitaRequest;
import cl.duoc.tacticaldev.dto.VisitaResponse;
import cl.duoc.tacticaldev.model.EstadoValidacion;
import cl.duoc.tacticaldev.service.VisitaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/visitas")
public class VisitaController {

    private final VisitaService service;

    public VisitaController(VisitaService service) {
        this.service = service;
    }

    @GetMapping
    public List<VisitaResponse> listar(@RequestParam(required = false) Long operarioId,
                                       @RequestParam(required = false) Long solicitudId,
                                       @RequestParam(required = false) EstadoValidacion estado) {
        return service.listar(operarioId, solicitudId, estado);
    }

    /** Detalle con la foto en Base64. */
    @GetMapping("/{id}")
    public VisitaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitaResponse registrar(@Valid @RequestBody VisitaRequest req) {
        return service.registrar(req);
    }

    @PutMapping("/{id}/validar")
    public VisitaResponse validar(@PathVariable Long id, @Valid @RequestBody ValidarRequest req) {
        return service.validar(id, req);
    }
}

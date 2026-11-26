package cl.duoc.tacticaldev.controller;

import cl.duoc.tacticaldev.dto.EquipoRequest;
import cl.duoc.tacticaldev.dto.EquipoResponse;
import cl.duoc.tacticaldev.dto.VisitaResponse;
import cl.duoc.tacticaldev.service.EquipoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    private final EquipoService service;

    public EquipoController(EquipoService service) {
        this.service = service;
    }

    @GetMapping
    public List<EquipoResponse> listar(@RequestParam(required = false) Long puntoVentaId,
                                       @RequestParam(required = false) Boolean enBodega) {
        return service.listar(puntoVentaId, enBodega);
    }

    @GetMapping("/{id}")
    public EquipoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/{id}/historial")
    public List<VisitaResponse> historial(@PathVariable Long id) {
        return service.historial(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoResponse crear(@Valid @RequestBody EquipoRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    public EquipoResponse actualizar(@PathVariable Long id, @Valid @RequestBody EquipoRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}

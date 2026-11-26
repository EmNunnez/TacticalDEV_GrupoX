package cl.duoc.tacticaldev.controller;

import cl.duoc.tacticaldev.dto.PuntoVentaRequest;
import cl.duoc.tacticaldev.dto.PuntoVentaResponse;
import cl.duoc.tacticaldev.service.PuntoVentaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/puntos-venta")
public class PuntoVentaController {

    private final PuntoVentaService service;

    public PuntoVentaController(PuntoVentaService service) {
        this.service = service;
    }

    @GetMapping
    public List<PuntoVentaResponse> listar(@RequestParam(required = false) Long clienteId) {
        return service.listar(clienteId);
    }

    @GetMapping("/{id}")
    public PuntoVentaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PuntoVentaResponse crear(@Valid @RequestBody PuntoVentaRequest req) {
        return service.crear(req);
    }

    @PutMapping("/{id}")
    public PuntoVentaResponse actualizar(@PathVariable Long id, @Valid @RequestBody PuntoVentaRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}

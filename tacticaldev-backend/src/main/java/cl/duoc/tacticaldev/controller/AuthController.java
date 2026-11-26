package cl.duoc.tacticaldev.controller;

import cl.duoc.tacticaldev.dto.LoginRequest;
import cl.duoc.tacticaldev.dto.UsuarioRequest;
import cl.duoc.tacticaldev.dto.UsuarioResponse;
import cl.duoc.tacticaldev.model.Rol;
import cl.duoc.tacticaldev.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService service;

    public AuthController(UsuarioService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public UsuarioResponse login(@Valid @RequestBody LoginRequest req) {
        return service.login(req);
    }

    /** Auto-registro desde la app: siempre crea un CLIENTE. */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registro(@Valid @RequestBody UsuarioRequest req) {
        UsuarioRequest comoCliente = new UsuarioRequest(req.nombre(), req.email(), req.password(),
                req.telefono(), Rol.CLIENTE, null, true);
        return service.crear(comoCliente);
    }
}

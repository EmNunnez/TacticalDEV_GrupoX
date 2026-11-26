package cl.duoc.tacticaldev.service;

import cl.duoc.tacticaldev.dto.LoginRequest;
import cl.duoc.tacticaldev.dto.UsuarioRequest;
import cl.duoc.tacticaldev.dto.UsuarioResponse;
import cl.duoc.tacticaldev.exception.ApiException;
import cl.duoc.tacticaldev.model.Rol;
import cl.duoc.tacticaldev.model.Usuario;
import cl.duoc.tacticaldev.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    /** Mínimo 8 caracteres, al menos una letra y un número (misma regla que la app). */
    private static final String REGEX_PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";

    private final UsuarioRepository repo;
    private final BCryptPasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repo, BCryptPasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public UsuarioResponse login(LoginRequest req) {
        Usuario u = repo.findByEmailIgnoreCase(req.email().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos"));
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        }
        if (!u.isActivo()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "El usuario está desactivado. Contacte a su supervisor");
        }
        return UsuarioResponse.from(u);
    }

    public List<UsuarioResponse> listar(Rol rol) {
        List<Usuario> lista = (rol == null) ? repo.findAll() : repo.findByRolOrderByNombreAsc(rol);
        return lista.stream().map(UsuarioResponse::from).toList();
    }

    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.from(buscar(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioRequest req) {
        if (repo.existsByEmailIgnoreCase(req.email().trim())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
        }
        validarPassword(req.password(), true);
        Usuario u = new Usuario();
        aplicar(u, req);
        u.setPasswordHash(encoder.encode(req.password()));
        return UsuarioResponse.from(repo.save(u));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest req) {
        Usuario u = buscar(id);
        repo.findByEmailIgnoreCase(req.email().trim())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ApiException(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
                });
        aplicar(u, req);
        if (req.password() != null && !req.password().isBlank()) {
            validarPassword(req.password(), false);
            u.setPasswordHash(encoder.encode(req.password()));
        }
        return UsuarioResponse.from(repo.save(u));
    }

    @Transactional
    public void eliminar(Long id) {
        Usuario u = buscar(id);
        if (u.getRol() == Rol.SUPERVISOR && repo.findByRolOrderByNombreAsc(Rol.SUPERVISOR).size() <= 1) {
            throw new ApiException(HttpStatus.CONFLICT, "No se puede eliminar al único supervisor");
        }
        repo.delete(u);
    }

    // ---------- helpers ----------

    private Usuario buscar(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario " + id + " no encontrado"));
    }

    private void aplicar(Usuario u, UsuarioRequest req) {
        if (req.rol() == Rol.OPERARIO && req.tipoOperario() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Un operario debe tener tipo: TECNICO, VENDEDOR o TRANSPORTISTA");
        }
        u.setNombre(req.nombre().trim());
        u.setEmail(req.email().trim().toLowerCase());
        u.setTelefono(req.telefono());
        u.setRol(req.rol());
        u.setTipoOperario(req.rol() == Rol.OPERARIO ? req.tipoOperario() : null);
        if (req.activo() != null) u.setActivo(req.activo());
    }

    private void validarPassword(String password, boolean obligatoria) {
        if (password == null || password.isBlank()) {
            if (obligatoria) throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria");
            return;
        }
        if (!password.matches(REGEX_PASSWORD)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "La contraseña debe tener al menos 8 caracteres, una letra y un número");
        }
    }
}

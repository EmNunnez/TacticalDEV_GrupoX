package cl.duoc.tacticaldev.dto;

import cl.duoc.tacticaldev.model.Rol;
import cl.duoc.tacticaldev.model.TipoOperario;
import cl.duoc.tacticaldev.model.Usuario;

/** Nunca se expone el hash de la contraseña. */
public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String telefono,
        Rol rol,
        TipoOperario tipoOperario,
        boolean activo
) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getEmail(), u.getTelefono(),
                u.getRol(), u.getTipoOperario(), u.isActivo());
    }
}

package cl.duoc.tacticaldev.dto;

import cl.duoc.tacticaldev.model.Rol;
import cl.duoc.tacticaldev.model.TipoOperario;
import jakarta.validation.constraints.*;

/**
 * Se usa para crear y actualizar. En la actualización el password es opcional
 * (si viene vacío se mantiene la contraseña actual).
 */
public record UsuarioRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(min = 3, max = 80, message = "El nombre debe tener entre 3 y 80 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Formato de correo inválido")
        String email,

        String password,

        @Pattern(regexp = "^$|^\\+?[0-9]{8,12}$", message = "Teléfono inválido (8 a 12 dígitos)")
        String telefono,

        @NotNull(message = "El rol es obligatorio")
        Rol rol,

        TipoOperario tipoOperario,

        Boolean activo
) {}

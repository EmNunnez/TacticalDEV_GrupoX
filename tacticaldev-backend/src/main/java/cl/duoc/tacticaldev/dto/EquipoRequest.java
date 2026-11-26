package cl.duoc.tacticaldev.dto;

import cl.duoc.tacticaldev.model.EstadoEquipo;
import cl.duoc.tacticaldev.model.TipoEquipo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EquipoRequest(
        @NotBlank(message = "El código es obligatorio")
        @Pattern(regexp = "^[A-Za-z]{2}-\\d{4}$", message = "El código debe tener formato XX-0000 (ej. VF-0001)")
        String codigo,
        @NotNull(message = "El tipo es obligatorio") TipoEquipo tipo,
        @NotBlank(message = "La marca es obligatoria") @Size(max = 50) String marca,
        @NotBlank(message = "El modelo es obligatorio") @Size(max = 50) String modelo,
        EstadoEquipo estado,
        /** null = en bodega */
        Long puntoVentaId
) {}

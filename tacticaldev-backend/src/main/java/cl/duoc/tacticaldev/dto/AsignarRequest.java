package cl.duoc.tacticaldev.dto;

import jakarta.validation.constraints.NotNull;

public record AsignarRequest(
        @NotNull(message = "Debe seleccionar un operario") Long operarioId,
        /** Solo para EQUIPO_NUEVO: equipo de bodega que se instalará. */
        Long equipoId
) {}

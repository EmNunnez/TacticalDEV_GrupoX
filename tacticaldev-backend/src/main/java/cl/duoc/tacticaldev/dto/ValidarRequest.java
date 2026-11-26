package cl.duoc.tacticaldev.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ValidarRequest(
        @NotNull(message = "Debe indicar si aprueba o rechaza") Boolean aprobada,
        @Size(max = 300) String comentario
) {}

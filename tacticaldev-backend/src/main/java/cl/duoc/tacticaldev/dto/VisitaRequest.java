package cl.duoc.tacticaldev.dto;

import cl.duoc.tacticaldev.model.EstadoEquipo;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record VisitaRequest(
        @NotBlank(message = "idLocal es obligatorio") @Size(max = 40) String idLocal,
        @NotNull(message = "La solicitud es obligatoria") Long solicitudId,
        @NotNull(message = "El operario es obligatorio") Long operarioId,
        @NotNull(message = "El estado del equipo es obligatorio") EstadoEquipo estadoEquipo,
        @Size(max = 500, message = "Observaciones: máximo 500 caracteres") String observaciones,
        @NotBlank(message = "La foto de evidencia es obligatoria") String fotoBase64,
        @NotNull(message = "La latitud es obligatoria") Double latitud,
        @NotNull(message = "La longitud es obligatoria") Double longitud,
        Double precisionMetros,
        boolean ubicacionSimulada,
        @NotNull(message = "La fecha de captura es obligatoria") LocalDateTime fechaCaptura,
        Double temperaturaAmbiente
) {}

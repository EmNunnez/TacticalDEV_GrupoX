package cl.duoc.tacticaldev.dto;

import cl.duoc.tacticaldev.model.TipoEquipo;
import cl.duoc.tacticaldev.model.TipoSolicitud;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SolicitudRequest(
        @NotNull(message = "El tipo de solicitud es obligatorio") TipoSolicitud tipo,
        @NotBlank(message = "La descripción es obligatoria")
        @Size(min = 10, max = 500, message = "La descripción debe tener entre 10 y 500 caracteres")
        String descripcion,
        @NotNull(message = "El cliente es obligatorio") Long clienteId,
        @NotNull(message = "El punto de venta es obligatorio") Long puntoVentaId,
        /** Obligatorio para REPARACION, RETIRO y REVISION. */
        Long equipoId,
        /** Obligatorio para EQUIPO_NUEVO. */
        TipoEquipo tipoEquipoSolicitado,
        String fotoClienteBase64
) {}

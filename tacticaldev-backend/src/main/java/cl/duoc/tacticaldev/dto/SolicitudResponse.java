package cl.duoc.tacticaldev.dto;

import cl.duoc.tacticaldev.model.EstadoSolicitud;
import cl.duoc.tacticaldev.model.Solicitud;
import cl.duoc.tacticaldev.model.TipoEquipo;
import cl.duoc.tacticaldev.model.TipoSolicitud;

import java.time.LocalDateTime;

public record SolicitudResponse(
        Long id, TipoSolicitud tipo, EstadoSolicitud estado, String descripcion,
        TipoEquipo tipoEquipoSolicitado, boolean tieneFotoCliente, String fotoClienteBase64,
        Long clienteId, String clienteNombre,
        PuntoVentaResponse puntoVenta, EquipoResponse equipo,
        Long operarioId, String operarioNombre,
        LocalDateTime fechaCreacion, LocalDateTime fechaAsignacion, LocalDateTime fechaCierre
) {
    /** @param conFoto false en listados para no enviar imágenes pesadas. */
    public static SolicitudResponse from(Solicitud s, boolean conFoto) {
        boolean tieneFoto = s.getFotoClienteBase64() != null && !s.getFotoClienteBase64().isBlank();
        return new SolicitudResponse(s.getId(), s.getTipo(), s.getEstado(), s.getDescripcion(),
                s.getTipoEquipoSolicitado(), tieneFoto, conFoto ? s.getFotoClienteBase64() : null,
                s.getClienteId(), s.getClienteNombre(),
                PuntoVentaResponse.from(s.getPuntoVenta()), EquipoResponse.from(s.getEquipo()),
                s.getOperarioId(), s.getOperarioNombre(),
                s.getFechaCreacion(), s.getFechaAsignacion(), s.getFechaCierre());
    }
}

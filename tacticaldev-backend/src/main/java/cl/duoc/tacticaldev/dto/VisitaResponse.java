package cl.duoc.tacticaldev.dto;

import cl.duoc.tacticaldev.model.*;

import java.time.LocalDateTime;

public record VisitaResponse(
        Long id, String idLocal, Long solicitudId, TipoSolicitud tipoSolicitud,
        String puntoVentaNombre, String equipoCodigo,
        Long operarioId, String operarioNombre,
        EstadoEquipo estadoEquipo, String observaciones, String fotoBase64,
        Double latitud, Double longitud, Double precisionMetros, boolean ubicacionSimulada,
        LocalDateTime fechaCaptura, LocalDateTime fechaRegistro,
        Double distanciaMetros, boolean ubicacionVerificada, String motivoVerificacion,
        Double temperaturaAmbiente,
        EstadoValidacion estadoValidacion, String comentarioSupervisor, LocalDateTime fechaValidacion
) {
    public static VisitaResponse from(Visita v, boolean conFoto) {
        Solicitud s = v.getSolicitud();
        return new VisitaResponse(v.getId(), v.getIdLocal(), s.getId(), s.getTipo(),
                s.getPuntoVenta().getNombre(), s.getEquipo() == null ? null : s.getEquipo().getCodigo(),
                v.getOperarioId(), v.getOperarioNombre(),
                v.getEstadoEquipo(), v.getObservaciones(), conFoto ? v.getFotoBase64() : null,
                v.getLatitud(), v.getLongitud(), v.getPrecisionMetros(), v.isUbicacionSimulada(),
                v.getFechaCaptura(), v.getFechaRegistro(),
                v.getDistanciaMetros(), v.isUbicacionVerificada(), v.getMotivoVerificacion(),
                v.getTemperaturaAmbiente(),
                v.getEstadoValidacion(), v.getComentarioSupervisor(), v.getFechaValidacion());
    }
}

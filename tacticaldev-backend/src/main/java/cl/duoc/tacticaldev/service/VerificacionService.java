package cl.duoc.tacticaldev.service;

import cl.duoc.tacticaldev.model.TipoSolicitud;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Reglas anti "visitas fantasma". Lógica pura (sin BD) para poder testearla fácilmente.
 */
@Service
public class VerificacionService {

    private static final double RADIO_TIERRA_M = 6_371_000.0;
    /** Precisión GPS máxima aceptada (metros). */
    public static final double PRECISION_MAXIMA_M = 150.0;
    /** Ventana para sincronizar visitas guardadas sin conexión. */
    public static final Duration VENTANA_OFFLINE = Duration.ofHours(72);
    /** Tolerancia de desfase del reloj del teléfono. */
    public static final Duration TOLERANCIA_FUTURO = Duration.ofMinutes(5);

    public record Resultado(double distanciaMetros, boolean verificada, String motivo) {}

    /** Distancia en metros entre dos coordenadas (fórmula de Haversine). */
    public double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return RADIO_TIERRA_M * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public Resultado verificar(double latFoto, double lonFoto, Double precision, boolean simulada,
                               double latPunto, double lonPunto, int radioMetros) {
        double d = Math.round(distanciaMetros(latFoto, lonFoto, latPunto, lonPunto) * 10.0) / 10.0;
        if (simulada) {
            return new Resultado(d, false, "Se detectó una ubicación simulada (GPS falso)");
        }
        if (precision != null && precision > PRECISION_MAXIMA_M) {
            return new Resultado(d, false, "Precisión GPS insuficiente (" + Math.round(precision) + " m)");
        }
        if (d > radioMetros) {
            return new Resultado(d, false, "Foto tomada a " + Math.round(d) + " m del punto de venta (máx. " + radioMetros + " m)");
        }
        return new Resultado(d, true, "Foto y GPS dentro del radio (" + Math.round(d) + " m)");
    }

    /** null si la fecha es válida; si no, el motivo del rechazo. */
    public String validarFechaCaptura(LocalDateTime fechaCaptura, LocalDateTime ahora) {
        if (fechaCaptura.isAfter(ahora.plus(TOLERANCIA_FUTURO))) {
            return "La fecha de la foto está en el futuro. Revise la hora del teléfono";
        }
        if (fechaCaptura.isBefore(ahora.minus(VENTANA_OFFLINE))) {
            return "La foto tiene más de 72 horas. Debe realizar una nueva visita";
        }
        return null;
    }

    /** Qué tipo de operario puede atender cada solicitud. */
    public boolean operarioCompatible(TipoSolicitud tipo, String tipoOperario) {
        if (tipoOperario == null) return false;
        return switch (tipo) {
            case REPARACION -> tipoOperario.equals("TECNICO");
            case EQUIPO_NUEVO, RETIRO -> Set.of("VENDEDOR", "TRANSPORTISTA").contains(tipoOperario);
            case REVISION -> true;
        };
    }
}

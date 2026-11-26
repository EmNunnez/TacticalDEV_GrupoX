package cl.duoc.tacticaldev.util

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Misma fórmula (Haversine) que usa el backend para verificar la visita. */
object GeoUtils {

    private const val RADIO_TIERRA_M = 6_371_000.0
    const val PRECISION_MAXIMA_M = 150.0

    fun distanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return RADIO_TIERRA_M * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun formatearDistancia(metros: Double): String =
        if (metros < 1000) "${metros.roundToInt()} m" else String.format(java.util.Locale.US, "%.1f km", metros / 1000)
}

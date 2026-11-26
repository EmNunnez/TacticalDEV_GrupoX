package cl.duoc.tacticaldev.util

/** Ubicación obtenida en el mismo instante en que se toma la foto. */
data class UbicacionCapturada(
    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Double?,
    /** true si Android detecta una app de GPS falso. */
    val esSimulada: Boolean
)

/** Abstracción del GPS para poder simularlo en las pruebas unitarias. */
interface LocationProvider {
    /** Devuelve null si no hay permiso o el GPS está apagado. */
    suspend fun obtenerUbicacionActual(): UbicacionCapturada?
}

/** Abstracción de la conversión de la foto a Base64 (comprimida). */
interface ImageEncoder {
    suspend fun codificar(uri: String): String?
}

package cl.duoc.tacticaldev.util

/** Traduce los códigos WMO que entrega Open-Meteo. */
object ClimaUtils {

    const val UMBRAL_ALTA_TEMPERATURA = 30.0

    fun descripcion(codigo: Int?): String = when (codigo) {
        0 -> "Despejado"
        1, 2 -> "Parcialmente nublado"
        3 -> "Nublado"
        45, 48 -> "Neblina"
        in 51..57 -> "Llovizna"
        in 61..67, in 80..82 -> "Lluvia"
        in 71..77, 85, 86 -> "Nieve"
        in 95..99 -> "Tormenta"
        null -> "Sin datos"
        else -> "Condición $codigo"
    }

    fun riesgoAltaTemperatura(temperatura: Double): Boolean = temperatura >= UMBRAL_ALTA_TEMPERATURA
}

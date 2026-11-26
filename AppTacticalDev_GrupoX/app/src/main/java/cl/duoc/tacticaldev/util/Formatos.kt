package cl.duoc.tacticaldev.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Formatos {
    private val ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val ISO_SEGUNDOS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    private val LEGIBLE = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")

    /** Fecha que se envía al servidor (sin zona, hora local de Chile). */
    fun fechaIso(fecha: LocalDateTime): String = fecha.format(ISO_SEGUNDOS)

    /** "2026-11-18T19:57:03.123" -> "18-11-2026 19:57". Si no se puede leer, devuelve el texto original. */
    fun fechaLegible(iso: String?): String {
        if (iso.isNullOrBlank()) return "-"
        return runCatching { LocalDateTime.parse(iso, ISO).format(LEGIBLE) }.getOrDefault(iso)
    }
}

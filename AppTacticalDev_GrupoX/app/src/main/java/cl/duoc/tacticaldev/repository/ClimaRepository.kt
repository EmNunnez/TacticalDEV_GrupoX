package cl.duoc.tacticaldev.repository

import cl.duoc.tacticaldev.data.model.Clima
import cl.duoc.tacticaldev.data.remote.ApiClima
import cl.duoc.tacticaldev.util.ClimaUtils
import cl.duoc.tacticaldev.util.safeCall

/** Consume la API externa Open-Meteo. */
class ClimaRepository(private val api: ApiClima) {

    suspend fun climaActual(latitud: Double, longitud: Double): Result<Clima> =
        safeCall { api.climaActual(latitud, longitud) }.mapCatching { r ->
            val actual = r.current ?: throw IllegalStateException("Open-Meteo no devolvió datos")
            val temp = actual.temperatura ?: throw IllegalStateException("Sin temperatura")
            Clima(
                temperatura = temp,
                humedad = actual.humedad,
                descripcion = ClimaUtils.descripcion(actual.codigoClima),
                riesgoAltaTemperatura = ClimaUtils.riesgoAltaTemperatura(temp)
            )
        }
}

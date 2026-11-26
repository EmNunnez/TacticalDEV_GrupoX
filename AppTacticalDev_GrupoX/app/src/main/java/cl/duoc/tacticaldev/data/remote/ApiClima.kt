package cl.duoc.tacticaldev.data.remote

import cl.duoc.tacticaldev.data.model.ClimaResponse
import retrofit2.http.GET
import retrofit2.http.Query

/** API EXTERNA: Open-Meteo. Independiente de los microservicios propios. */
interface ApiClima {
    @GET("v1/forecast")
    suspend fun climaActual(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        @Query("current") campos: String = "temperature_2m,relative_humidity_2m,weather_code",
        @Query("timezone") zona: String = "auto"
    ): ClimaResponse
}

package cl.duoc.tacticaldev.data.model

import com.google.gson.annotations.SerializedName

/** Respuesta de la API externa Open-Meteo (https://open-meteo.com, sin API key). */
data class ClimaResponse(
    val latitude: Double?,
    val longitude: Double?,
    val current: ClimaActual?
)

data class ClimaActual(
    val time: String?,
    @SerializedName("temperature_2m") val temperatura: Double?,
    @SerializedName("relative_humidity_2m") val humedad: Int?,
    @SerializedName("weather_code") val codigoClima: Int?
)

/** Modelo que usa la UI. */
data class Clima(
    val temperatura: Double,
    val humedad: Int?,
    val descripcion: String,
    /** Sobre 30 °C aumenta el riesgo de falla en equipos de frío. */
    val riesgoAltaTemperatura: Boolean
)

package cl.duoc.tacticaldev.util

import cl.duoc.tacticaldev.data.model.ErrorResponse
import com.google.gson.Gson
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** Error de negocio devuelto por un microservicio (400, 404, 409...). */
class ApiException(message: String, val codigo: Int) : Exception(message)

/** No se pudo contactar al servidor (sin internet, servidor apagado, timeout). */
class SinConexionException : IOException("Sin conexión con el servidor. Verifique su red")

private val gson = Gson()

/** Extrae el campo "mensaje" del cuerpo de error JSON. */
internal fun mensajeDeError(cuerpo: String?, codigo: Int): String {
    val mensaje = runCatching { gson.fromJson(cuerpo, ErrorResponse::class.java)?.mensaje }.getOrNull()
    return if (!mensaje.isNullOrBlank()) mensaje else "Error del servidor ($codigo)"
}

/** Ejecuta una llamada Retrofit y convierte las excepciones en un Result con mensaje legible. */
suspend fun <T> safeCall(bloque: suspend () -> T): Result<T> = try {
    Result.success(bloque())
} catch (e: HttpException) {
    Result.failure(ApiException(mensajeDeError(e.response()?.errorBody()?.string(), e.code()), e.code()))
} catch (e: IOException) {
    Result.failure(SinConexionException())
} catch (e: Exception) {
    Result.failure(Exception(e.message ?: "Error inesperado"))
}

/** Para endpoints DELETE que devuelven Response<Unit> (204 sin cuerpo). */
suspend fun safeCallSinCuerpo(bloque: suspend () -> Response<Unit>): Result<Unit> =
    safeCall { bloque() }.mapCatching { r ->
        if (!r.isSuccessful) throw ApiException(mensajeDeError(r.errorBody()?.string(), r.code()), r.code())
    }

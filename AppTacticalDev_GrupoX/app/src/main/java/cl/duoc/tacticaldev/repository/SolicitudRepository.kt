package cl.duoc.tacticaldev.repository

import cl.duoc.tacticaldev.data.model.*
import cl.duoc.tacticaldev.data.remote.ApiOperaciones
import cl.duoc.tacticaldev.util.SinConexionException
import cl.duoc.tacticaldev.util.safeCall
import cl.duoc.tacticaldev.util.safeCallSinCuerpo

class SolicitudRepository(private val api: ApiOperaciones) {

    /**
     * Copia en memoria de las últimas solicitudes descargadas. Permite que el operario
     * abra el formulario de visita aunque en terreno se haya quedado sin señal.
     */
    private val cache = mutableMapOf<Long, Solicitud>()

    suspend fun listar(
        clienteId: Long? = null,
        operarioId: Long? = null,
        estado: EstadoSolicitud? = null
    ): Result<List<Solicitud>> =
        safeCall { api.listarSolicitudes(clienteId, operarioId, estado) }
            .onSuccess { lista -> lista.forEach { cache[it.id] = it } }

    suspend fun obtener(id: Long): Result<Solicitud> {
        val r = safeCall { api.obtenerSolicitud(id) }.onSuccess { cache[id] = it }
        val enCache = cache[id]
        return if (r.exceptionOrNull() is SinConexionException && enCache != null) Result.success(enCache) else r
    }

    suspend fun crear(req: SolicitudRequest): Result<Solicitud> = safeCall { api.crearSolicitud(req) }

    suspend fun actualizarDescripcion(id: Long, descripcion: String): Result<Solicitud> =
        safeCall { api.actualizarSolicitud(id, SolicitudUpdateRequest(descripcion.trim())) }

    suspend fun asignar(id: Long, operarioId: Long, equipoId: Long?): Result<Solicitud> =
        safeCall { api.asignarSolicitud(id, AsignarRequest(operarioId, equipoId)) }

    suspend fun eliminar(id: Long): Result<Unit> =
        safeCallSinCuerpo { api.eliminarSolicitud(id) }.onSuccess { cache.remove(id) }
}

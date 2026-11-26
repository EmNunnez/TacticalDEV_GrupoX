package cl.duoc.tacticaldev.repository

import cl.duoc.tacticaldev.data.local.VisitaPendienteDao
import cl.duoc.tacticaldev.data.local.VisitaPendienteEntity
import cl.duoc.tacticaldev.data.model.EstadoValidacion
import cl.duoc.tacticaldev.data.model.ValidarRequest
import cl.duoc.tacticaldev.data.model.Visita
import cl.duoc.tacticaldev.data.model.VisitaRequest
import cl.duoc.tacticaldev.data.remote.ApiOperaciones
import cl.duoc.tacticaldev.util.SinConexionException
import cl.duoc.tacticaldev.util.safeCall
import kotlinx.coroutines.flow.Flow

/** Resultado de registrar una visita en terreno. */
sealed class ResultadoRegistro {
    /** Llegó al servidor, que ya verificó el GPS. */
    data class Enviada(val visita: Visita) : ResultadoRegistro()
    /** Sin internet: quedó guardada en el teléfono y se enviará después. */
    data object GuardadaSinConexion : ResultadoRegistro()
}

data class ResumenSincronizacion(val enviadas: Int, val rechazadas: Int, val pendientes: Int)

/**
 * Estrategia "offline first": la visita se guarda SIEMPRE primero en Room
 * y solo se borra del teléfono cuando el servidor la recibió.
 */
class VisitaRepository(
    private val api: ApiOperaciones,
    private val dao: VisitaPendienteDao,
    private val reloj: () -> Long = { System.currentTimeMillis() }
) {

    fun pendientes(operarioId: Long): Flow<List<VisitaPendienteEntity>> = dao.observar(operarioId)

    suspend fun solicitudesConPendiente(operarioId: Long): Set<Long> =
        dao.solicitudesConPendiente(operarioId).toSet()

    suspend fun registrar(req: VisitaRequest, puntoVentaNombre: String): Result<ResultadoRegistro> {
        dao.insertar(VisitaPendienteEntity.desde(req, puntoVentaNombre, reloj()))
        val envio = safeCall { api.registrarVisita(req) }
        return envio.fold(
            onSuccess = { visita ->
                dao.eliminar(req.idLocal)
                Result.success(ResultadoRegistro.Enviada(visita))
            },
            onFailure = { error ->
                if (error is SinConexionException) {
                    Result.success(ResultadoRegistro.GuardadaSinConexion)
                } else {
                    // El servidor la rechazó por una regla de negocio: reintentar no la arregla
                    dao.eliminar(req.idLocal)
                    Result.failure(error)
                }
            }
        )
    }

    /** Envía todas las visitas guardadas sin conexión. */
    suspend fun sincronizar(operarioId: Long): ResumenSincronizacion {
        var enviadas = 0
        var rechazadas = 0
        var pendientes = 0
        for (p in dao.obtenerTodas(operarioId)) {
            safeCall { api.registrarVisita(p.aRequest()) }
                .onSuccess {
                    dao.eliminar(p.idLocal)
                    enviadas++
                }
                .onFailure { e ->
                    if (e is SinConexionException) {
                        pendientes++
                    } else {
                        dao.eliminar(p.idLocal)
                        rechazadas++
                    }
                }
        }
        return ResumenSincronizacion(enviadas, rechazadas, pendientes)
    }

    suspend fun listar(operarioId: Long? = null, estado: EstadoValidacion? = null): Result<List<Visita>> =
        safeCall { api.listarVisitas(operarioId = operarioId, estado = estado) }

    suspend fun obtener(id: Long): Result<Visita> = safeCall { api.obtenerVisita(id) }

    suspend fun validar(id: Long, aprobada: Boolean, comentario: String): Result<Visita> =
        safeCall { api.validarVisita(id, ValidarRequest(aprobada, comentario.trim().ifBlank { null })) }
}

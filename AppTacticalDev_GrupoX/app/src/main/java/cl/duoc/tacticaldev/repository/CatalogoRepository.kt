package cl.duoc.tacticaldev.repository

import cl.duoc.tacticaldev.data.model.*
import cl.duoc.tacticaldev.data.remote.ApiOperaciones
import cl.duoc.tacticaldev.util.safeCall
import cl.duoc.tacticaldev.util.safeCallSinCuerpo

/** Datos maestros: puntos de venta y equipos (activos). */
class CatalogoRepository(private val api: ApiOperaciones) {

    suspend fun puntos(clienteId: Long? = null): Result<List<PuntoVenta>> = safeCall { api.listarPuntos(clienteId) }
    suspend fun crearPunto(req: PuntoVentaRequest): Result<PuntoVenta> = safeCall { api.crearPunto(req) }
    suspend fun actualizarPunto(id: Long, req: PuntoVentaRequest): Result<PuntoVenta> = safeCall { api.actualizarPunto(id, req) }
    suspend fun eliminarPunto(id: Long): Result<Unit> = safeCallSinCuerpo { api.eliminarPunto(id) }

    suspend fun equipos(puntoVentaId: Long? = null, enBodega: Boolean? = null): Result<List<Equipo>> =
        safeCall { api.listarEquipos(puntoVentaId, enBodega) }
    suspend fun crearEquipo(req: EquipoRequest): Result<Equipo> = safeCall { api.crearEquipo(req) }
    suspend fun actualizarEquipo(id: Long, req: EquipoRequest): Result<Equipo> = safeCall { api.actualizarEquipo(id, req) }
    suspend fun eliminarEquipo(id: Long): Result<Unit> = safeCallSinCuerpo { api.eliminarEquipo(id) }
    suspend fun historial(equipoId: Long): Result<List<Visita>> = safeCall { api.historialEquipo(equipoId) }
}

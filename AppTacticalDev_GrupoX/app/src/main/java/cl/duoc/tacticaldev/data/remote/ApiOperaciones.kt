package cl.duoc.tacticaldev.data.remote

import cl.duoc.tacticaldev.data.model.*
import retrofit2.Response
import retrofit2.http.*

/** Endpoints de puntos de venta, equipos, solicitudes y visitas del backend en AWS (puerto 8080). */
interface ApiOperaciones {

    // ---------- Puntos de venta ----------
    @GET("api/puntos-venta")
    suspend fun listarPuntos(@Query("clienteId") clienteId: Long? = null): List<PuntoVenta>

    @POST("api/puntos-venta")
    suspend fun crearPunto(@Body req: PuntoVentaRequest): PuntoVenta

    @PUT("api/puntos-venta/{id}")
    suspend fun actualizarPunto(@Path("id") id: Long, @Body req: PuntoVentaRequest): PuntoVenta

    @DELETE("api/puntos-venta/{id}")
    suspend fun eliminarPunto(@Path("id") id: Long): Response<Unit>

    // ---------- Equipos ----------
    @GET("api/equipos")
    suspend fun listarEquipos(
        @Query("puntoVentaId") puntoVentaId: Long? = null,
        @Query("enBodega") enBodega: Boolean? = null
    ): List<Equipo>

    @GET("api/equipos/{id}/historial")
    suspend fun historialEquipo(@Path("id") id: Long): List<Visita>

    @POST("api/equipos")
    suspend fun crearEquipo(@Body req: EquipoRequest): Equipo

    @PUT("api/equipos/{id}")
    suspend fun actualizarEquipo(@Path("id") id: Long, @Body req: EquipoRequest): Equipo

    @DELETE("api/equipos/{id}")
    suspend fun eliminarEquipo(@Path("id") id: Long): Response<Unit>

    // ---------- Solicitudes ----------
    @GET("api/solicitudes")
    suspend fun listarSolicitudes(
        @Query("clienteId") clienteId: Long? = null,
        @Query("operarioId") operarioId: Long? = null,
        @Query("estado") estado: EstadoSolicitud? = null
    ): List<Solicitud>

    @GET("api/solicitudes/{id}")
    suspend fun obtenerSolicitud(@Path("id") id: Long): Solicitud

    @POST("api/solicitudes")
    suspend fun crearSolicitud(@Body req: SolicitudRequest): Solicitud

    @PUT("api/solicitudes/{id}")
    suspend fun actualizarSolicitud(@Path("id") id: Long, @Body req: SolicitudUpdateRequest): Solicitud

    @PUT("api/solicitudes/{id}/asignar")
    suspend fun asignarSolicitud(@Path("id") id: Long, @Body req: AsignarRequest): Solicitud

    @DELETE("api/solicitudes/{id}")
    suspend fun eliminarSolicitud(@Path("id") id: Long): Response<Unit>

    // ---------- Visitas ----------
    @GET("api/visitas")
    suspend fun listarVisitas(
        @Query("operarioId") operarioId: Long? = null,
        @Query("solicitudId") solicitudId: Long? = null,
        @Query("estado") estado: EstadoValidacion? = null
    ): List<Visita>

    @GET("api/visitas/{id}")
    suspend fun obtenerVisita(@Path("id") id: Long): Visita

    @POST("api/visitas")
    suspend fun registrarVisita(@Body req: VisitaRequest): Visita

    @PUT("api/visitas/{id}/validar")
    suspend fun validarVisita(@Path("id") id: Long, @Body req: ValidarRequest): Visita
}

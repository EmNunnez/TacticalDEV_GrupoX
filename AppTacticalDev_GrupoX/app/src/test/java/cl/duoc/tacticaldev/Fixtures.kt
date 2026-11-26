package cl.duoc.tacticaldev

import cl.duoc.tacticaldev.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import io.kotest.core.spec.style.StringSpec
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

/** Datos ficticios reutilizados por todas las pruebas. */
object Fixtures {

    fun usuario(id: Long = 1, rol: Rol = Rol.SUPERVISOR, tipo: TipoOperario? = null, activo: Boolean = true) =
        Usuario(id, "Usuario $id", "u$id@tacticaldev.cl", null, rol, tipo, activo)

    fun sesion(id: Long = 1, rol: Rol = Rol.OPERARIO) = Sesion(id, "Usuario $id", "u$id@tacticaldev.cl", rol, null)

    /** Coordenadas aproximadas de Duoc UC Plaza Oeste, radio 100 m. */
    fun punto(id: Long = 1, clienteId: Long = 5) =
        PuntoVenta(id, "Minimarket $id", "Av. Ficticia $id", "Cerrillos", -33.5167, -70.7160, 100, clienteId)

    fun equipo(
        id: Long = 1,
        tipo: TipoEquipo = TipoEquipo.VITRINA_FREEZER,
        estado: EstadoEquipo = EstadoEquipo.OPERATIVO,
        puntoId: Long? = 1
    ) = Equipo(id, "VF-000$id", tipo, "FrioMax", "VF-300", estado, puntoId, puntoId?.let { "Minimarket $it" })

    fun solicitud(
        id: Long = 10,
        tipo: TipoSolicitud = TipoSolicitud.REPARACION,
        estado: EstadoSolicitud = EstadoSolicitud.PENDIENTE,
        equipo: Equipo? = equipo(),
        tipoEquipoSolicitado: TipoEquipo? = null,
        operarioId: Long? = null
    ) = Solicitud(
        id, tipo, estado, "La vitrina no enfría bien", tipoEquipoSolicitado, false, null,
        5, "Cliente 5", punto(), equipo, operarioId, operarioId?.let { "Operario $it" },
        "2026-11-18T10:00:00", null, null
    )

    fun visita(
        id: Long = 100,
        validacion: EstadoValidacion = EstadoValidacion.PENDIENTE,
        verificada: Boolean = true
    ) = Visita(
        id, "uuid-$id", 10, TipoSolicitud.REPARACION, "Minimarket 1", "VF-0001", 2, "Operario 2",
        EstadoEquipo.OPERATIVO, "Todo ok", null, -33.5167, -70.7160, 10.0, false,
        "2026-11-18T10:00:00", "2026-11-18T10:00:05", 12.0, verificada, "Foto y GPS dentro del radio",
        22.5, validacion, null, null
    )

    fun httpError(codigo: Int, mensaje: String): HttpException = HttpException(
        Response.error<Any>(codigo, """{"mensaje":"$mensaje"}""".toResponseBody("application/json".toMediaType()))
    )
}

/** Base para specs que prueban ViewModels: reemplaza Dispatchers.Main. */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class ViewModelSpec(body: ViewModelSpec.() -> Unit = {}) : StringSpec() {
    init {
        beforeTest { Dispatchers.setMain(UnconfinedTestDispatcher()) }
        afterTest { Dispatchers.resetMain() }
        body()
    }
}

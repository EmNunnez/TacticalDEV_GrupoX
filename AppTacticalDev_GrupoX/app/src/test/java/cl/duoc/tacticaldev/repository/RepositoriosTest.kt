package cl.duoc.tacticaldev.repository

import cl.duoc.tacticaldev.Fixtures
import cl.duoc.tacticaldev.data.local.SesionStore
import cl.duoc.tacticaldev.data.local.VisitaPendienteDao
import cl.duoc.tacticaldev.data.local.VisitaPendienteEntity
import cl.duoc.tacticaldev.data.model.*
import cl.duoc.tacticaldev.data.remote.ApiClima
import cl.duoc.tacticaldev.data.remote.ApiOperaciones
import cl.duoc.tacticaldev.data.remote.ApiUsuarios
import cl.duoc.tacticaldev.util.ApiException
import cl.duoc.tacticaldev.util.SinConexionException
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import retrofit2.Response
import java.io.IOException

class AuthRepositoryTest : StringSpec({
    val api = mockk<ApiUsuarios>()
    val store = mockk<SesionStore>(relaxed = true)
    every { store.sesion } returns flowOf(Fixtures.sesion())
    val repo = AuthRepository(api, store)

    "login exitoso guarda la sesión en DataStore" {
        val u = Fixtures.usuario(rol = Rol.CLIENTE)
        coEvery { api.login(LoginRequest("cliente@minimarket.cl", "Cliente1234")) } returns u
        repo.login(" cliente@minimarket.cl ", "Cliente1234").getOrNull() shouldBe u
        coVerify { store.guardar(u) }
    }

    "login fallido no guarda sesión" {
        clearMocks(store, answers = false)
        coEvery { api.login(any()) } throws Fixtures.httpError(401, "Correo o contraseña incorrectos")
        repo.login("x@x.cl", "mala").exceptionOrNull()?.message shouldBe "Correo o contraseña incorrectos"
        coVerify(exactly = 0) { store.guardar(any()) }
    }

    "registro siempre crea un CLIENTE y limpia el teléfono vacío" {
        val slot = slot<UsuarioRequest>()
        coEvery { api.registro(capture(slot)) } returns Fixtures.usuario(rol = Rol.CLIENTE)
        repo.registrarCliente(" Don Pepe ", "pepe@mini.cl", " ", "Clave1234").isSuccess shouldBe true
        slot.captured.rol shouldBe Rol.CLIENTE
        slot.captured.nombre shouldBe "Don Pepe"
        slot.captured.telefono shouldBe null
    }

    "sesión y cierre de sesión" {
        repo.sesion.first()?.id shouldBe 1L
        repo.cerrarSesion()
        coVerify { store.cerrar() }
    }
})

class UsuarioYCatalogoRepositoryTest : StringSpec({
    val apiU = mockk<ApiUsuarios>()
    val apiO = mockk<ApiOperaciones>()
    val usuarios = UsuarioRepository(apiU)
    val catalogo = CatalogoRepository(apiO)
    val req = UsuarioRequest("Tomás", "t@t.cl", "Opera1234", null, Rol.OPERARIO, TipoOperario.TECNICO)

    "CRUD de usuarios" {
        coEvery { apiU.listar(Rol.OPERARIO) } returns listOf(Fixtures.usuario(2, Rol.OPERARIO))
        coEvery { apiU.crear(req) } returns Fixtures.usuario(9)
        coEvery { apiU.actualizar(9, req) } returns Fixtures.usuario(9)
        coEvery { apiU.eliminar(9) } returns Response.success(Unit)
        usuarios.listar(Rol.OPERARIO).getOrThrow() shouldHaveSize 1
        usuarios.crear(req).getOrThrow().id shouldBe 9L
        usuarios.actualizar(9, req).isSuccess shouldBe true
        usuarios.eliminar(9).isSuccess shouldBe true
    }

    "CRUD de puntos de venta y equipos" {
        val pReq = PuntoVentaRequest("Local", "Calle 1", "Maipú", -33.5, -70.7, 100, 5)
        val eReq = EquipoRequest("VF-0009", TipoEquipo.VITRINA_FREEZER, "FrioMax", "X", EstadoEquipo.OPERATIVO, null)
        coEvery { apiO.listarPuntos(5) } returns listOf(Fixtures.punto())
        coEvery { apiO.crearPunto(pReq) } returns Fixtures.punto()
        coEvery { apiO.actualizarPunto(1, pReq) } returns Fixtures.punto()
        coEvery { apiO.eliminarPunto(1) } returns Response.success(Unit)
        coEvery { apiO.listarEquipos(null, true) } returns listOf(Fixtures.equipo(puntoId = null))
        coEvery { apiO.crearEquipo(eReq) } returns Fixtures.equipo(9)
        coEvery { apiO.actualizarEquipo(9, eReq) } returns Fixtures.equipo(9)
        coEvery { apiO.eliminarEquipo(9) } returns Response.success(Unit)
        coEvery { apiO.historialEquipo(1) } returns listOf(Fixtures.visita())

        catalogo.puntos(5).getOrThrow() shouldHaveSize 1
        catalogo.crearPunto(pReq).isSuccess shouldBe true
        catalogo.actualizarPunto(1, pReq).isSuccess shouldBe true
        catalogo.eliminarPunto(1).isSuccess shouldBe true
        catalogo.equipos(enBodega = true).getOrThrow().first().puntoVentaId shouldBe null
        catalogo.crearEquipo(eReq).isSuccess shouldBe true
        catalogo.actualizarEquipo(9, eReq).isSuccess shouldBe true
        catalogo.eliminarEquipo(9).isSuccess shouldBe true
        catalogo.historial(1).getOrThrow() shouldHaveSize 1
    }
})

class SolicitudRepositoryTest : StringSpec({
    val api = mockk<ApiOperaciones>()
    val repo = SolicitudRepository(api)
    val sol = Fixtures.solicitud(id = 10, estado = EstadoSolicitud.ASIGNADA, operarioId = 2)

    "sin conexión devuelve la solicitud desde la caché si ya se había descargado" {
        coEvery { api.listarSolicitudes(null, 2, EstadoSolicitud.ASIGNADA) } returns listOf(sol)
        repo.listar(operarioId = 2, estado = EstadoSolicitud.ASIGNADA).getOrThrow() shouldHaveSize 1
        coEvery { api.obtenerSolicitud(10) } throws IOException("sin red")
        repo.obtener(10).getOrThrow() shouldBe sol
    }

    "sin conexión y sin caché devuelve el error" {
        coEvery { api.obtenerSolicitud(99) } throws IOException("sin red")
        repo.obtener(99).exceptionOrNull().shouldBeInstanceOf<SinConexionException>()
    }

    "crear, actualizar, asignar y eliminar" {
        val req = SolicitudRequest(TipoSolicitud.REPARACION, "No enfría nada", 5, 1, 1, null, null)
        coEvery { api.crearSolicitud(req) } returns sol
        coEvery { api.actualizarSolicitud(10, SolicitudUpdateRequest("Nueva descripción")) } returns sol
        coEvery { api.asignarSolicitud(10, AsignarRequest(2, null)) } returns sol
        coEvery { api.eliminarSolicitud(10) } returns Response.success(Unit)
        coEvery { api.obtenerSolicitud(10) } returns sol

        repo.crear(req).isSuccess shouldBe true
        repo.actualizarDescripcion(10, " Nueva descripción ").isSuccess shouldBe true
        repo.asignar(10, 2, null).isSuccess shouldBe true
        repo.obtener(10).isSuccess shouldBe true
        repo.eliminar(10).isSuccess shouldBe true
    }
})

class VisitaRepositoryTest : StringSpec({
    val api = mockk<ApiOperaciones>()
    val dao = mockk<VisitaPendienteDao>(relaxed = true)
    val repo = VisitaRepository(api, dao) { 1_000L }
    val req = VisitaRequest(
        "uuid-1", 10, 2, EstadoEquipo.OPERATIVO, null, "BASE64", -33.5167, -70.716, 8.0, false,
        "2026-11-18T10:00:00", 21.0
    )

    beforeTest { clearMocks(api, dao) }

    "con internet: se envía y se borra de la cola local" {
        coEvery { api.registrarVisita(req) } returns Fixtures.visita()
        val r = repo.registrar(req, "Minimarket 1").getOrThrow()
        r.shouldBeInstanceOf<ResultadoRegistro.Enviada>()
        coVerifyOrder {
            dao.insertar(VisitaPendienteEntity.desde(req, "Minimarket 1", 1_000L))
            dao.eliminar("uuid-1")
        }
    }

    "sin internet: queda guardada en Room" {
        coEvery { api.registrarVisita(req) } throws IOException()
        repo.registrar(req, "Minimarket 1").getOrThrow() shouldBe ResultadoRegistro.GuardadaSinConexion
        coVerify(exactly = 0) { dao.eliminar(any()) }
    }

    "rechazo del servidor: se descarta y se informa el motivo" {
        coEvery { api.registrarVisita(req) } throws Fixtures.httpError(409, "La solicitud no está pendiente de visita")
        val e = repo.registrar(req, "Minimarket 1").exceptionOrNull()
        e.shouldBeInstanceOf<ApiException>()
        coVerify { dao.eliminar("uuid-1") }
    }

    "sincronizar cuenta enviadas, rechazadas y pendientes" {
        val a = VisitaPendienteEntity.desde(req.copy(idLocal = "a"), "M", 1)
        val b = VisitaPendienteEntity.desde(req.copy(idLocal = "b"), "M", 2)
        val c = VisitaPendienteEntity.desde(req.copy(idLocal = "c"), "M", 3)
        coEvery { dao.obtenerTodas(2) } returns listOf(a, b, c)
        coEvery { api.registrarVisita(match { it.idLocal == "a" }) } returns Fixtures.visita()
        coEvery { api.registrarVisita(match { it.idLocal == "b" }) } throws Fixtures.httpError(400, "Foto con más de 72 horas")
        coEvery { api.registrarVisita(match { it.idLocal == "c" }) } throws IOException()
        repo.sincronizar(2) shouldBe ResumenSincronizacion(enviadas = 1, rechazadas = 1, pendientes = 1)
        coVerify { dao.eliminar("a"); dao.eliminar("b") }
        coVerify(exactly = 0) { dao.eliminar("c") }
    }

    "la entidad local se convierte de vuelta en request" {
        VisitaPendienteEntity.desde(req, "M", 5).aRequest() shouldBe req
    }

    "consultas, validación y pendientes" {
        coEvery { dao.solicitudesConPendiente(2) } returns listOf(10L, 10L)
        every { dao.observar(2) } returns flowOf(emptyList())
        coEvery { api.listarVisitas(2, null, null) } returns listOf(Fixtures.visita())
        coEvery { api.obtenerVisita(100) } returns Fixtures.visita()
        coEvery { api.validarVisita(100, ValidarRequest(true, null)) } returns Fixtures.visita(validacion = EstadoValidacion.VALIDADA)

        repo.solicitudesConPendiente(2) shouldBe setOf(10L)
        repo.pendientes(2).first() shouldHaveSize 0
        repo.listar(operarioId = 2).getOrThrow() shouldHaveSize 1
        repo.obtener(100).isSuccess shouldBe true
        repo.validar(100, true, "  ").getOrThrow().estadoValidacion shouldBe EstadoValidacion.VALIDADA
    }
})

class ClimaRepositoryTest : StringSpec({
    val api = mockk<ApiClima>()
    val repo = ClimaRepository(api)

    "convierte la respuesta de Open-Meteo" {
        coEvery { api.climaActual(-33.5, -70.7, any(), any()) } returns
                ClimaResponse(-33.5, -70.7, ClimaActual("2026-11-18T12:00", 31.2, 40, 0))
        val c = repo.climaActual(-33.5, -70.7).getOrThrow()
        c.temperatura shouldBe 31.2
        c.descripcion shouldBe "Despejado"
        c.riesgoAltaTemperatura shouldBe true
    }

    "respuesta sin datos es un error" {
        coEvery { api.climaActual(any(), any(), any(), any()) } returns ClimaResponse(null, null, null)
        repo.climaActual(0.0, 0.0).isFailure shouldBe true
        coEvery { api.climaActual(any(), any(), any(), any()) } returns
                ClimaResponse(null, null, ClimaActual(null, null, null, null))
        repo.climaActual(0.0, 0.0).isFailure shouldBe true
    }

    "sin internet falla sin romper la app" {
        coEvery { api.climaActual(any(), any(), any(), any()) } throws IOException()
        repo.climaActual(0.0, 0.0).exceptionOrNull().shouldBeInstanceOf<SinConexionException>()
    }
})

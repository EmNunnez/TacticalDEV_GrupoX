package cl.duoc.tacticaldev

import cl.duoc.tacticaldev.data.local.SesionStore
import cl.duoc.tacticaldev.data.model.*
import cl.duoc.tacticaldev.data.remote.ApiOperaciones
import cl.duoc.tacticaldev.data.remote.ApiUsuarios
import cl.duoc.tacticaldev.repository.*
import cl.duoc.tacticaldev.ui.navigation.Rutas
import cl.duoc.tacticaldev.util.*
import cl.duoc.tacticaldev.viewmodel.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import retrofit2.Response
import java.io.IOException

/*
 * Pruebas complementarias enfocadas en las RAMAS (if/when/?:) que las pruebas
 * principales no recorrían: casos de error, límites de rangos y dobles clics
 * mientras una operación sigue en curso.
 */

// ======================= util =======================

class UtilsRamasTest : StringSpec({
    "safeCall usa un texto genérico si la excepción no trae mensaje" {
        safeCall<Int> { throw IllegalStateException() }.exceptionOrNull()?.message shouldBe "Error inesperado"
    }
    "safeCallSinCuerpo sin conexión no evalúa la respuesta" {
        safeCallSinCuerpo { throw IOException("sin red") }.exceptionOrNull().shouldNotBeNull()
            .shouldBeInstance<SinConexionException>()
    }
    "límites de latitud, longitud y radio" {
        Validaciones.latitud("-90.1") shouldBe "Debe estar entre -90 y 90"
        Validaciones.latitud("90,5") shouldBe "Debe estar entre -90 y 90"
        Validaciones.latitud("-33,5").shouldBeNull()
        Validaciones.longitud("-180.5") shouldBe "Debe estar entre -180 y 180"
        Validaciones.longitud("181") shouldBe "Debe estar entre -180 y 180"
        Validaciones.longitud("-70,7").shouldBeNull()
        Validaciones.radio("19") shouldBe "Debe estar entre 20 y 1000 metros"
        Validaciones.radio("1001") shouldBe "Debe estar entre 20 y 1000 metros"
        Validaciones.radio("20").shouldBeNull()
    }
    "todos los códigos WMO agrupados" {
        ClimaUtils.descripcion(1) shouldBe "Parcialmente nublado"
        ClimaUtils.descripcion(48) shouldBe "Neblina"
        ClimaUtils.descripcion(51) shouldBe "Llovizna"
        ClimaUtils.descripcion(57) shouldBe "Llovizna"
        ClimaUtils.descripcion(61) shouldBe "Lluvia"
        ClimaUtils.descripcion(80) shouldBe "Lluvia"
        ClimaUtils.descripcion(82) shouldBe "Lluvia"
        ClimaUtils.descripcion(71) shouldBe "Nieve"
        ClimaUtils.descripcion(77) shouldBe "Nieve"
        ClimaUtils.descripcion(85) shouldBe "Nieve"
        ClimaUtils.descripcion(86) shouldBe "Nieve"
        ClimaUtils.descripcion(95) shouldBe "Tormenta"
        ClimaUtils.descripcion(99) shouldBe "Tormenta"
        ClimaUtils.descripcion(4) shouldBe "Condición 4"
        ClimaUtils.descripcion(70) shouldBe "Condición 70"
    }
})

private inline fun <reified T : Throwable> Throwable.shouldBeInstance(): T {
    (this is T) shouldBe true
    return this as T
}

// ======================= repository =======================

class RepositoriosRamasTest : StringSpec({
    "listar solicitudes con error no llena la caché" {
        val api = mockk<ApiOperaciones>()
        val repo = SolicitudRepository(api)
        coEvery { api.listarSolicitudes(null, null, null) } throws IOException()
        repo.listar().isFailure shouldBe true
        coEvery { api.obtenerSolicitud(10) } throws IOException()
        repo.obtener(10).isFailure shouldBe true
    }
    "eliminar solicitud fallido conserva la caché" {
        val api = mockk<ApiOperaciones>()
        val repo = SolicitudRepository(api)
        val s = Fixtures.solicitud(10)
        coEvery { api.listarSolicitudes(5, null, null) } returns listOf(s)
        repo.listar(clienteId = 5)
        coEvery { api.eliminarSolicitud(10) } throws Fixtures.httpError(409, "Ya fue asignada")
        repo.eliminar(10).exceptionOrNull()?.message shouldBe "Ya fue asignada"
        coEvery { api.obtenerSolicitud(10) } throws IOException()
        repo.obtener(10).getOrNull() shouldBe s
    }
    "registro sin teléfono lo envía como null" {
        val api = mockk<ApiUsuarios>()
        val store = mockk<SesionStore>(relaxed = true)
        val slot = slot<UsuarioRequest>()
        coEvery { api.registro(capture(slot)) } returns Fixtures.usuario(rol = Rol.CLIENTE)
        AuthRepository(api, store).registrarCliente("Pepe", "p@p.cl", "   ", "Clave1234")
        slot.captured.telefono.shouldBeNull()
        coEvery { api.registro(capture(slot)) } returns Fixtures.usuario(rol = Rol.CLIENTE)
        AuthRepository(api, store).registrarCliente("Pepe", "p@p.cl", " +56912345678 ", "Clave1234")
        slot.captured.telefono shouldBe "+56912345678"
    }
    "validar visita con comentario en blanco lo envía como null" {
        val api = mockk<ApiOperaciones>()
        val repo = VisitaRepository(api, mockk(relaxed = true))
        val slot = slot<ValidarRequest>()
        coEvery { api.validarVisita(100, capture(slot)) } returns Fixtures.visita()
        repo.validar(100, true, "  ")
        slot.captured.comentario.shouldBeNull()
        repo.validar(100, false, " Foto borrosa ")
        slot.captured.comentario shouldBe "Foto borrosa"
    }
})

// ======================= viewmodel =======================

class OperarioRamasTest : ViewModelSpec({
    fun deps(): Pair<SolicitudRepository, VisitaRepository> {
        val sol = mockk<SolicitudRepository>()
        val vis = mockk<VisitaRepository>()
        every { vis.pendientes(2) } returns MutableStateFlow(emptyList())
        coEvery { vis.solicitudesConPendiente(2) } returns emptySet()
        return sol to vis
    }

    "errores de carga mantienen las listas anteriores" {
        val (sol, vis) = deps()
        coEvery { sol.listar(operarioId = 2, estado = EstadoSolicitud.ASIGNADA) } returns Result.failure(Exception("Sin conexión"))
        coEvery { vis.listar(operarioId = 2) } returns Result.failure(Exception("Sin conexión"))
        val vm = OperarioViewModel(sol, vis, 2)
        vm.estado.value.error shouldBe "Sin conexión"
        vm.estado.value.asignadas.shouldBeEmpty()
        vm.estado.value.visitas.shouldBeEmpty()
    }

    "solo rechazadas: mensaje y recarga" {
        val (sol, vis) = deps()
        coEvery { sol.listar(operarioId = 2, estado = EstadoSolicitud.ASIGNADA) } returns Result.success(emptyList())
        coEvery { vis.listar(operarioId = 2) } returns Result.success(emptyList())
        coEvery { vis.sincronizar(2) } returns ResumenSincronizacion(0, 1, 0)
        val vm = OperarioViewModel(sol, vis, 2)
        vm.sincronizar()
        vm.estado.value.mensaje shouldBe "0 enviada(s), 1 rechazada(s) por el servidor"
        coVerify(exactly = 2) { vis.listar(operarioId = 2) }
    }

    "no sincroniza dos veces a la vez" {
        val (sol, vis) = deps()
        coEvery { sol.listar(operarioId = 2, estado = EstadoSolicitud.ASIGNADA) } returns Result.success(emptyList())
        coEvery { vis.listar(operarioId = 2) } returns Result.success(emptyList())
        val espera = CompletableDeferred<ResumenSincronizacion>()
        coEvery { vis.sincronizar(2) } coAnswers { espera.await() }
        val vm = OperarioViewModel(sol, vis, 2)
        vm.sincronizar()
        vm.estado.value.sincronizando shouldBe true
        vm.sincronizar()
        espera.complete(ResumenSincronizacion(0, 0, 0))
        vm.estado.value.sincronizando shouldBe false
        coVerify(exactly = 1) { vis.sincronizar(2) }
    }
})

class ClienteRamasTest : ViewModelSpec({
    "error al cargar puntos y equipos de un local" {
        val sol = mockk<SolicitudRepository>()
        val cat = mockk<CatalogoRepository>()
        coEvery { sol.listar(clienteId = 5) } returns Result.success(listOf(Fixtures.solicitud()))
        coEvery { cat.puntos(5) } returns Result.failure(Exception("backend caído"))
        val vm = ClienteViewModel(sol, cat, 5)
        vm.estado.value.error shouldBe "backend caído"
        vm.estado.value.puntos.shouldBeEmpty()

        coEvery { cat.puntos(5) } returns Result.success(listOf(Fixtures.punto()))
        coEvery { cat.equipos(puntoVentaId = 1) } returns Result.failure(Exception("timeout"))
        vm.cargar()
        vm.estado.value.error.shouldBeNull()
        vm.estado.value.equipos.shouldBeEmpty()
    }
    "error al listar solicitudes conserva las anteriores" {
        val sol = mockk<SolicitudRepository>()
        val cat = mockk<CatalogoRepository>()
        coEvery { sol.listar(clienteId = 5) } returns Result.success(listOf(Fixtures.solicitud()))
        coEvery { cat.puntos(5) } returns Result.success(emptyList())
        val vm = ClienteViewModel(sol, cat, 5)
        coEvery { sol.listar(clienteId = 5) } returns Result.failure(Exception("Sin conexión"))
        vm.cargar()
        vm.estado.value.error shouldBe "Sin conexión"
        vm.estado.value.solicitudes.size shouldBe 1
    }
})

class AuthRamasTest : ViewModelSpec({
    "login: solo falta la contraseña o solo el correo" {
        val vm = LoginViewModel(mockk())
        vm.onEmail("a@a.cl")
        vm.validar() shouldBe false
        vm.estado.value.emailError.shouldBeNull()
        vm.estado.value.passwordError.shouldNotBeNull()
        vm.onEmail("")
        vm.onPasswordChange("Clave1234")
        vm.validar() shouldBe false
        vm.estado.value.passwordError.shouldBeNull()
    }
    "login: doble clic mientras carga no repite la llamada" {
        val auth = mockk<AuthRepository>()
        val espera = CompletableDeferred<Result<Usuario>>()
        coEvery { auth.login(any(), any()) } coAnswers { espera.await() }
        val vm = LoginViewModel(auth)
        vm.onEmail("a@a.cl")
        vm.onPasswordChange("Clave1234")
        vm.login()
        vm.login()
        espera.complete(Result.success(Fixtures.usuario()))
        vm.estado.value.exito shouldBe true
        coVerify(exactly = 1) { auth.login(any(), any()) }
    }
    "registro: doble clic mientras carga no repite la llamada" {
        val auth = mockk<AuthRepository>()
        val espera = CompletableDeferred<Result<Usuario>>()
        coEvery { auth.registrarCliente(any(), any(), any(), any()) } coAnswers { espera.await() }
        coEvery { auth.login(any(), any()) } returns Result.success(Fixtures.usuario(rol = Rol.CLIENTE))
        val vm = RegistroViewModel(auth)
        vm.onNombre("Minimarket Don Pepe")
        vm.onEmail("pepe@minimarket.cl")
        vm.onTelefono("+56912345678")
        vm.onPassword("Cliente1234")
        vm.onConfirmar("Cliente1234")
        vm.onTerminos(true)
        vm.registrar()
        vm.registrar()
        espera.complete(Result.success(Fixtures.usuario(rol = Rol.CLIENTE)))
        vm.estado.value.cargando shouldBe false
        coVerify(exactly = 1) { auth.registrarCliente(any(), any(), any(), any()) }
    }
})

private fun LoginViewModel.onEmail(v: String) = onEmailChange(v)

class DetalleVisitaRamasTest : ViewModelSpec({
    "puedeValidar según el estado de la visita" {
        DetalleVisitaUiState().puedeValidar shouldBe false
        DetalleVisitaUiState(visita = Fixtures.visita(validacion = EstadoValidacion.PENDIENTE)).puedeValidar shouldBe true
        DetalleVisitaUiState(visita = Fixtures.visita(validacion = EstadoValidacion.SOSPECHOSA)).puedeValidar shouldBe true
        DetalleVisitaUiState(visita = Fixtures.visita(validacion = EstadoValidacion.VALIDADA)).puedeValidar shouldBe false
        DetalleVisitaUiState(visita = Fixtures.visita(validacion = EstadoValidacion.RECHAZADA)).puedeValidar shouldBe false
    }
    "reglas del comentario" {
        val repo = mockk<VisitaRepository>()
        coEvery { repo.obtener(1) } returns Result.failure(Exception("no encontrada"))
        val vm = DetalleVisitaViewModel(repo, 1)
        // Sin visita cargada: aprobar sin comentario es válido
        vm.validarComentario(true) shouldBe true
        vm.onComentario("x".repeat(301))
        vm.validarComentario(true) shouldBe false
        vm.estado.value.comentarioError shouldBe "Máximo 300 caracteres"
        vm.validarComentario(false) shouldBe false
    }
    "aprobar una visita sospechosa con justificación y doble clic" {
        val repo = mockk<VisitaRepository>()
        val sospechosa = Fixtures.visita(validacion = EstadoValidacion.SOSPECHOSA)
        coEvery { repo.obtener(100) } returns Result.success(sospechosa)
        val espera = CompletableDeferred<Result<Visita>>()
        coEvery { repo.validar(100, true, "Local cambió de número") } coAnswers { espera.await() }
        val vm = DetalleVisitaViewModel(repo, 100)
        vm.validarComentario(true) shouldBe false
        vm.onComentario("Local cambió de número")
        vm.validar(true)
        vm.estado.value.procesando shouldBe true
        vm.validar(true)
        espera.complete(Result.success(sospechosa.copy(estadoValidacion = EstadoValidacion.VALIDADA)))
        vm.estado.value.terminado shouldBe true
        coVerify(exactly = 1) { repo.validar(any(), any(), any()) }
    }
})

class SupervisorRamasTest : ViewModelSpec({
    "errores parciales de carga" {
        val sol = mockk<SolicitudRepository>()
        val vis = mockk<VisitaRepository>()
        val usu = mockk<UsuarioRepository>()
        val cat = mockk<CatalogoRepository>()
        coEvery { sol.listar() } returns Result.success(emptyList())
        coEvery { vis.listar(estado = EstadoValidacion.PENDIENTE) } returns Result.failure(Exception("pendientes caídas"))
        coEvery { vis.listar(estado = EstadoValidacion.SOSPECHOSA) } returns Result.failure(Exception("x"))
        coEvery { usu.listar(Rol.OPERARIO) } returns Result.success(emptyList())
        coEvery { cat.equipos(enBodega = true) } returns Result.failure(Exception("bodega"))
        val vm = SupervisorViewModel(sol, vis, usu, cat)
        vm.estado.value.error shouldBe "pendientes caídas"
        vm.estado.value.visitasPorRevisar.shouldBeEmpty()

        coEvery { sol.listar() } returns Result.failure(Exception("solicitudes caídas"))
        coEvery { usu.listar(Rol.OPERARIO) } returns Result.failure(Exception("usuarios caídos"))
        vm.cargar()
        vm.estado.value.error shouldBe "solicitudes caídas"

        coEvery { sol.listar() } returns Result.success(emptyList())
        coEvery { vis.listar(estado = EstadoValidacion.PENDIENTE) } returns Result.success(emptyList())
        vm.cargar()
        vm.estado.value.error shouldBe "usuarios caídos"
    }
})

class GestionRamasTest : ViewModelSpec({
    class Deps {
        val cat = mockk<CatalogoRepository>()
        val usu = mockk<UsuarioRepository>()
        val sol = mockk<SolicitudRepository>()
        val gps = mockk<LocationProvider>()
    }
    val conTelefono = Fixtures.usuario(7, Rol.CLIENTE).copy(telefono = "+56911112222")

    fun crear(d: Deps = Deps()): Pair<GestionViewModel, Deps> {
        coEvery { d.cat.puntos() } returns Result.success(listOf(Fixtures.punto()))
        coEvery { d.cat.equipos() } returns Result.success(listOf(Fixtures.equipo()))
        coEvery { d.usu.listar() } returns Result.success(listOf(conTelefono))
        return GestionViewModel(d.cat, d.usu, d.sol, d.gps) to d
    }

    "errores de equipos o usuarios al cargar" {
        val d = Deps()
        coEvery { d.cat.puntos() } returns Result.success(emptyList())
        coEvery { d.cat.equipos() } returns Result.failure(Exception("equipos caídos"))
        coEvery { d.usu.listar() } returns Result.failure(Exception("usuarios caídos"))
        val vm = GestionViewModel(d.cat, d.usu, d.sol, d.gps)
        vm.estado.value.error shouldBe "equipos caídos"
        coEvery { d.cat.equipos() } returns Result.success(emptyList())
        vm.cargar()
        vm.estado.value.error shouldBe "usuarios caídos"
    }

    "preparar usuario con teléfono y entidades inexistentes" {
        val (vm) = crear()
        vm.prepararUsuario(7)
        vm.estado.value.usuarioForm.telefono shouldBe "+56911112222"
        vm.prepararUsuario(7)
        vm.prepararUsuario(999)
        vm.prepararPunto(999)
        vm.prepararEquipo(999)
        vm.estado.value.formCargado shouldBe "u7"
    }

    "equipo retirado en bodega es válido y formulario inválido no se guarda" {
        val (vm, d) = crear()
        vm.validarEquipo(EquipoForm("VF-0009", TipoEquipo.CONSERVADORA, "FrioMax", "C1", EstadoEquipo.RETIRADO, null))
            .shouldNotContainKey("punto")
        vm.guardarEquipo(1, EquipoForm())
        vm.estado.value.erroresForm shouldContainKey "codigo"
        coVerify(exactly = 0) { d.cat.actualizarEquipo(any(), any()) }
    }

    "usuario: teléfono inválido y edición sin cambiar contraseña" {
        val (vm, d) = crear()
        vm.validarUsuario(UsuarioForm(telefono = "12"), esNuevo = true) shouldContainKey "telefono"
        vm.guardarUsuario(7, UsuarioForm())
        coVerify(exactly = 0) { d.usu.actualizar(any(), any()) }

        val slot = slot<UsuarioRequest>()
        coEvery { d.usu.actualizar(7, capture(slot)) } returns Result.success(conTelefono)
        vm.guardarUsuario(7, UsuarioForm("Cliente Siete", "c7@tacticaldev.cl", "", "", Rol.CLIENTE, TipoOperario.TECNICO))
        slot.captured.telefono.shouldBeNull()
        slot.captured.password.shouldBeNull()
        slot.captured.tipoOperario.shouldBeNull()
    }

    "no guarda dos veces mientras sigue guardando" {
        val (vm, d) = crear()
        val espera = CompletableDeferred<Result<PuntoVenta>>()
        coEvery { d.cat.crearPunto(any()) } coAnswers { espera.await() }
        val form = PuntoForm("Local", "Calle 1", "Maipú", "-33.5", "-70.7", "100", 5)
        vm.guardarPunto(null, form)
        vm.estado.value.guardando shouldBe true
        vm.guardarPunto(null, form)
        espera.complete(Result.success(Fixtures.punto()))
        vm.estado.value.guardado shouldBe true
        coVerify(exactly = 1) { d.cat.crearPunto(any()) }
    }
})

class VisitaRamasTest : ViewModelSpec({
    val solicitud = Fixtures.solicitud(10, estado = EstadoSolicitud.ASIGNADA, operarioId = 2)
    val cercaSinPrecision = UbicacionCapturada(-33.5168, -70.7161, null, false)

    class Deps {
        val sol = mockk<SolicitudRepository>()
        val vis = mockk<VisitaRepository>()
        val clima = mockk<ClimaRepository>()
        val gps = mockk<LocationProvider>()
        val enc = mockk<ImageEncoder>()
    }

    fun crear(d: Deps = Deps()): Pair<VisitaViewModel, Deps> {
        coEvery { d.sol.obtener(10) } returns Result.success(solicitud)
        coEvery { d.clima.climaActual(any(), any()) } returns Result.failure(Exception("API clima caída"))
        coEvery { d.enc.codificar(any()) } returns "BASE64"
        coEvery { d.gps.obtenerUbicacionActual() } returns cercaSinPrecision
        return VisitaViewModel(d.sol, d.vis, d.clima, d.gps, d.enc, 2, 10, generarId = { "uuid" }) to d
    }

    "puedeEnviar exige captura válida, sin envío ni GPS en curso" {
        val ok = CapturaEvidencia("u", 0.0, 0.0, null, false, "f", 1.0, true, "ok")
        VisitaUiState(captura = ok).puedeEnviar shouldBe true
        VisitaUiState(captura = ok, enviando = true).puedeEnviar shouldBe false
        VisitaUiState(captura = ok, capturandoGps = true).puedeEnviar shouldBe false
        VisitaUiState(captura = ok.copy(dentroDelRadio = false)).puedeEnviar shouldBe false
        VisitaUiState().puedeEnviar shouldBe false
    }

    "sin solicitud cargada la foto se ignora" {
        val d = Deps()
        coEvery { d.sol.obtener(10) } returns Result.failure(Exception("no encontrada"))
        val vm = VisitaViewModel(d.sol, d.vis, d.clima, d.gps, d.enc, 2, 10)
        vm.onFotoTomada("content://foto")
        vm.estado.value.captura.shouldBeNull()
        coVerify(exactly = 0) { d.gps.obtenerUbicacionActual() }
    }

    "GPS sin precisión informada, sin clima y sin observaciones" {
        val (vm, d) = crear()
        vm.onFotoTomada("content://foto")
        vm.estado.value.captura!!.dentroDelRadio shouldBe true
        vm.estado.value.climaNoDisponible shouldBe true
        vm.onEstadoEquipo(EstadoEquipo.OPERATIVO)
        val slot = slot<VisitaRequest>()
        val espera = CompletableDeferred<Result<ResultadoRegistro>>()
        coEvery { d.vis.registrar(capture(slot), any()) } coAnswers { espera.await() }
        vm.enviar()
        vm.estado.value.enviando shouldBe true
        vm.enviar()
        espera.complete(Result.failure(Exception("Error del servidor (500)")))
        vm.estado.value.error shouldBe "Error del servidor (500)"
        slot.captured.observaciones.shouldBeNull()
        slot.captured.temperaturaAmbiente.shouldBeNull()
        slot.captured.precisionMetros.shouldBeNull()
        coVerify(exactly = 1) { d.vis.registrar(any(), any()) }
    }
})

class NuevaSolicitudRamasTest : ViewModelSpec({
    "doble clic en enviar no crea dos solicitudes" {
        val sol = mockk<SolicitudRepository>()
        val cat = mockk<CatalogoRepository>()
        coEvery { cat.puntos(5) } returns Result.success(listOf(Fixtures.punto()))
        coEvery { cat.equipos(puntoVentaId = 1) } returns Result.success(listOf(Fixtures.equipo()))
        val espera = CompletableDeferred<Result<Solicitud>>()
        coEvery { sol.crear(any()) } coAnswers { espera.await() }
        val vm = NuevaSolicitudViewModel(sol, cat, mockk(), 5)
        vm.seleccionarEquipo(1)
        vm.onDescripcion("La vitrina no enfría desde ayer")
        vm.enviar()
        vm.enviar()
        espera.complete(Result.success(Fixtures.solicitud()))
        vm.estado.value.creada shouldBe true
        coVerify(exactly = 1) { sol.crear(any()) }
    }
})

// ======================= navegación =======================

class RutasTest : StringSpec({
    "las rutas se inicializan sin errores (causa del crash en AppNavigation)" {
        Rutas.Splash.ruta shouldBe "splash"
        Rutas.publicas shouldBe setOf("splash", "login", "registro")
    }
    "inicio según el rol" {
        Rutas.inicioPara(Rol.CLIENTE) shouldBe "cliente"
        Rutas.inicioPara(Rol.OPERARIO) shouldBe "operario"
        Rutas.inicioPara(Rol.SUPERVISOR) shouldBe "supervisor"
    }
    "rutas con parámetros" {
        Rutas.RegistrarVisita.crear(10) shouldBe "operario/visita/10"
        Rutas.FormPunto.crear(null) shouldBe "supervisor/punto/0"
        Rutas.FormPunto.crear(3) shouldBe "supervisor/punto/3"
        Rutas.FormEquipo.crear(null) shouldBe "supervisor/equipo/0"
        Rutas.FormEquipo.crear(4) shouldBe "supervisor/equipo/4"
        Rutas.FormUsuario.crear(null) shouldBe "supervisor/usuario/0"
        Rutas.FormUsuario.crear(5) shouldBe "supervisor/usuario/5"
        Rutas.DetalleVisita.crear(100) shouldBe "visita/100"
        Rutas.Historial.crear(1, "VF-0001") shouldBe "historial/1/VF-0001"
        Rutas.ClienteHome.ruta shouldBe "cliente"
        Rutas.NuevaSolicitud.ruta shouldBe "cliente/nueva-solicitud"
        Rutas.OperarioHome.ruta shouldBe "operario"
        Rutas.SupervisorHome.ruta shouldBe "supervisor"
        Rutas.Login.ruta shouldBe "login"
        Rutas.Registro.ruta shouldBe "registro"
    }
})

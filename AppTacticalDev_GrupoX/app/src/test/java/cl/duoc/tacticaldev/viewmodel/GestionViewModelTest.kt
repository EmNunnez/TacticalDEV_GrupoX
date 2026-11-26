package cl.duoc.tacticaldev.viewmodel

import cl.duoc.tacticaldev.Fixtures
import cl.duoc.tacticaldev.ViewModelSpec
import cl.duoc.tacticaldev.data.model.*
import cl.duoc.tacticaldev.repository.CatalogoRepository
import cl.duoc.tacticaldev.repository.SolicitudRepository
import cl.duoc.tacticaldev.repository.UsuarioRepository
import cl.duoc.tacticaldev.util.LocationProvider
import cl.duoc.tacticaldev.util.UbicacionCapturada
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldContainKeys
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*

class GestionViewModelTest : ViewModelSpec({
    val cliente = Fixtures.usuario(5, Rol.CLIENTE)
    val tecnico = Fixtures.usuario(2, Rol.OPERARIO, TipoOperario.TECNICO)
    val conFalla = Fixtures.equipo(2, estado = EstadoEquipo.CON_FALLA)

    class Deps {
        val cat = mockk<CatalogoRepository>()
        val usu = mockk<UsuarioRepository>()
        val sol = mockk<SolicitudRepository>()
        val gps = mockk<LocationProvider>()
    }

    fun crear(d: Deps = Deps()): Pair<GestionViewModel, Deps> {
        coEvery { d.cat.puntos() } returns Result.success(listOf(Fixtures.punto()))
        coEvery { d.cat.equipos() } returns Result.success(listOf(Fixtures.equipo(), conFalla, Fixtures.equipo(3, puntoId = null)))
        coEvery { d.usu.listar() } returns Result.success(listOf(cliente, tecnico))
        return GestionViewModel(d.cat, d.usu, d.sol, d.gps) to d
    }

    "carga datos maestros y calcula indicadores" {
        val (vm) = crear()
        vm.estado.value.clientes shouldBe listOf(cliente)
        vm.estado.value.equiposConFalla shouldBe 1
    }

    "error de carga" {
        val d = Deps()
        coEvery { d.cat.puntos() } returns Result.failure(Exception("backend caído"))
        coEvery { d.cat.equipos() } returns Result.success(emptyList())
        coEvery { d.usu.listar() } returns Result.success(emptyList())
        GestionViewModel(d.cat, d.usu, d.sol, d.gps).estado.value.error shouldBe "backend caído"
    }

    // ---------------- Puntos de venta ----------------

    "validación de punto de venta" {
        val (vm) = crear()
        vm.validarPunto(PuntoForm(radio = "5")).shouldContainKeys("nombre", "direccion", "comuna", "latitud", "longitud", "radio", "cliente")
        vm.validarPunto(PuntoForm("Local", "Calle 1", "Maipú", "-33.5", "-70.7", "100", 5)).shouldBeEmpty()
    }

    "crear y editar punto (CREATE/UPDATE)" {
        val (vm, d) = crear()
        vm.guardarPunto(null, PuntoForm())
        vm.estado.value.erroresForm.shouldContainKey("nombre")
        val form = PuntoForm("Local", "Calle 1", "Maipú", "-33,5", "-70.7", "150", 5)
        val slot = slot<PuntoVentaRequest>()
        coEvery { d.cat.crearPunto(capture(slot)) } returns Result.success(Fixtures.punto())
        vm.guardarPunto(null, form)
        vm.estado.value.guardado shouldBe true
        slot.captured.latitud shouldBe -33.5
        slot.captured.radioMetros shouldBe 150
        vm.reiniciarFormulario()
        coEvery { d.cat.actualizarPunto(1, any()) } returns Result.failure(Exception("Punto de venta 1 no encontrado"))
        vm.guardarPunto(1, form)
        vm.estado.value.error shouldBe "Punto de venta 1 no encontrado"
        vm.estado.value.guardando shouldBe false
    }

    "eliminar punto (DELETE) ok y con error" {
        val (vm, d) = crear()
        coEvery { d.cat.eliminarPunto(1) } returns Result.success(Unit)
        vm.eliminarPunto(1)
        vm.estado.value.mensaje shouldBe "Punto de venta eliminado"
        coEvery { d.cat.eliminarPunto(1) } returns Result.failure(Exception("Tiene equipos instalados"))
        vm.eliminarPunto(1)
        vm.estado.value.error shouldBe "Tiene equipos instalados"
    }

    "usar ubicación actual rellena latitud y longitud" {
        val (vm, d) = crear()
        coEvery { d.gps.obtenerUbicacionActual() } returns UbicacionCapturada(-33.1, -70.2, 5.0, false)
        vm.usarUbicacionActual()
        vm.estado.value.puntoForm.latitud shouldBe "-33.1"
        vm.estado.value.puntoForm.longitud shouldBe "-70.2"
        coEvery { d.gps.obtenerUbicacionActual() } returns null
        vm.usarUbicacionActual()
        vm.estado.value.error.shouldNotBeNull()
        vm.estado.value.puntoForm.latitud shouldBe "-33.1"
    }

    "preparar formularios de edición una sola vez" {
        val (vm) = crear()
        vm.prepararPunto(1)
        vm.estado.value.puntoForm.nombre shouldBe "Minimarket 1"
        vm.onPuntoForm(vm.estado.value.puntoForm.copy(nombre = "Editado"))
        vm.prepararPunto(1)
        vm.estado.value.puntoForm.nombre shouldBe "Editado"
        vm.prepararPunto(99)
        vm.prepararEquipo(2)
        vm.estado.value.equipoForm.estado shouldBe EstadoEquipo.CON_FALLA
        vm.prepararEquipo(2)
        vm.prepararEquipo(99)
        vm.prepararUsuario(2)
        vm.estado.value.usuarioForm.tipoOperario shouldBe TipoOperario.TECNICO
        vm.estado.value.usuarioForm.password shouldBe ""
        vm.prepararUsuario(2)
        vm.prepararUsuario(99)
        vm.onEquipoForm(EquipoForm(codigo = "X"))
        vm.onUsuarioForm(UsuarioForm(nombre = "Y"))
        vm.estado.value.equipoForm.codigo shouldBe "X"
        vm.estado.value.usuarioForm.nombre shouldBe "Y"
    }

    // ---------------- Equipos ----------------

    "validación de equipo" {
        val (vm) = crear()
        vm.validarEquipo(EquipoForm()).shouldContainKeys("codigo", "tipo", "marca", "modelo")
        vm.validarEquipo(EquipoForm("VF-0001", TipoEquipo.VITRINA_FREEZER, "M", "X", EstadoEquipo.RETIRADO, 1))
            .shouldContainKey("punto")
    }

    "crear, editar y eliminar equipo" {
        val (vm, d) = crear()
        val form = EquipoForm("vf-0010", TipoEquipo.VITRINA_FREEZER, "FrioMax", "VF-300", EstadoEquipo.OPERATIVO, null)
        val slot = slot<EquipoRequest>()
        coEvery { d.cat.crearEquipo(capture(slot)) } returns Result.success(Fixtures.equipo(10))
        vm.guardarEquipo(null, form)
        slot.captured.codigo shouldBe "VF-0010"
        coEvery { d.cat.actualizarEquipo(10, any()) } returns Result.success(Fixtures.equipo(10))
        vm.reiniciarFormulario()
        vm.guardarEquipo(10, form)
        vm.estado.value.guardado shouldBe true
        coEvery { d.cat.eliminarEquipo(10) } returns Result.success(Unit)
        vm.eliminarEquipo(10)
        vm.estado.value.mensaje shouldBe "Equipo eliminado"
    }

    "programar revisión periódica de un equipo instalado" {
        val (vm, d) = crear()
        vm.programarRevision(Fixtures.equipo(3, puntoId = null))
        vm.estado.value.error!! shouldContain "instalados"
        val slot = slot<SolicitudRequest>()
        coEvery { d.sol.crear(capture(slot)) } returns Result.success(Fixtures.solicitud(50, TipoSolicitud.REVISION))
        vm.programarRevision(Fixtures.equipo(1))
        slot.captured.tipo shouldBe TipoSolicitud.REVISION
        slot.captured.clienteId shouldBe 5L
        vm.estado.value.mensaje!! shouldContain "#50"
        coEvery { d.sol.crear(any()) } returns Result.failure(Exception("Ya existe una solicitud abierta"))
        vm.programarRevision(Fixtures.equipo(1))
        vm.estado.value.error shouldBe "Ya existe una solicitud abierta"
    }

    // ---------------- Usuarios ----------------

    "validación de usuario nuevo vs edición" {
        val (vm) = crear()
        vm.validarUsuario(UsuarioForm(rol = Rol.OPERARIO), esNuevo = true)
            .shouldContainKeys("nombre", "email", "password", "tipoOperario")
        vm.validarUsuario(UsuarioForm("Tomás", "t@t.cl", "", "", null), esNuevo = false).shouldContainKey("rol")
        vm.validarUsuario(UsuarioForm("Tomás", "t@t.cl", "", "", Rol.SUPERVISOR), esNuevo = false).shouldBeEmpty()
    }

    "crear, editar y eliminar usuario" {
        val (vm, d) = crear()
        val form = UsuarioForm("Tomás Técnico", "t@t.cl", "", "Opera1234", Rol.OPERARIO, TipoOperario.TECNICO)
        val slot = slot<UsuarioRequest>()
        coEvery { d.usu.crear(capture(slot)) } returns Result.success(tecnico)
        vm.guardarUsuario(null, form)
        slot.captured.tipoOperario shouldBe TipoOperario.TECNICO
        slot.captured.telefono.shouldBeNull()

        val slot2 = slot<UsuarioRequest>()
        coEvery { d.usu.actualizar(2, capture(slot2)) } returns Result.success(tecnico)
        vm.reiniciarFormulario()
        vm.guardarUsuario(2, form.copy(password = "", rol = Rol.SUPERVISOR))
        slot2.captured.password.shouldBeNull()
        slot2.captured.tipoOperario.shouldBeNull()

        coEvery { d.usu.eliminar(2) } returns Result.success(Unit)
        vm.eliminarUsuario(2)
        vm.estado.value.mensaje shouldBe "Usuario eliminado"
        vm.limpiarMensajes()
        vm.estado.value.mensaje.shouldBeNull()
    }
})

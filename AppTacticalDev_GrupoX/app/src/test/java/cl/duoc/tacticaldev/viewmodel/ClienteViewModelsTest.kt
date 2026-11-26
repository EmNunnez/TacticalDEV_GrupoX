package cl.duoc.tacticaldev.viewmodel

import cl.duoc.tacticaldev.Fixtures
import cl.duoc.tacticaldev.ViewModelSpec
import cl.duoc.tacticaldev.data.model.*
import cl.duoc.tacticaldev.repository.CatalogoRepository
import cl.duoc.tacticaldev.repository.SolicitudRepository
import cl.duoc.tacticaldev.util.ImageEncoder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.*

class ClienteViewModelTest : ViewModelSpec({
    val pendiente = Fixtures.solicitud(10, estado = EstadoSolicitud.PENDIENTE)
    val cerrada = Fixtures.solicitud(11, estado = EstadoSolicitud.VALIDADA)

    fun crear(): Triple<ClienteViewModel, SolicitudRepository, CatalogoRepository> {
        val sol = mockk<SolicitudRepository>()
        val cat = mockk<CatalogoRepository>()
        coEvery { sol.listar(clienteId = 5) } returns Result.success(listOf(pendiente, cerrada))
        coEvery { cat.puntos(5) } returns Result.success(listOf(Fixtures.punto()))
        coEvery { cat.equipos(puntoVentaId = 1) } returns Result.success(listOf(Fixtures.equipo()))
        return Triple(ClienteViewModel(sol, cat, 5), sol, cat)
    }

    "al iniciar carga solicitudes, puntos y equipos" {
        val (vm) = crear()
        with(vm.estado.value) {
            solicitudes shouldHaveSize 2
            puntos shouldHaveSize 1
            equipos shouldHaveSize 1
            abiertas shouldBe 1
            cargando shouldBe false
        }
    }

    "error de red se muestra al usuario" {
        val sol = mockk<SolicitudRepository>()
        val cat = mockk<CatalogoRepository>()
        coEvery { sol.listar(clienteId = 5) } returns Result.failure(Exception("Sin conexión"))
        coEvery { cat.puntos(5) } returns Result.success(emptyList())
        val vm = ClienteViewModel(sol, cat, 5)
        vm.estado.value.error shouldBe "Sin conexión"
        vm.limpiarMensajes()
        vm.estado.value.error.shouldBeNull()
    }

    "cancelar (DELETE) solo se permite en PENDIENTE" {
        val (vm, sol) = crear()
        vm.cancelar(cerrada)
        vm.estado.value.error.shouldNotBeNull()
        coEvery { sol.eliminar(10) } returns Result.success(Unit)
        vm.cancelar(pendiente)
        vm.estado.value.solicitudes.map { it.id } shouldBe listOf(11L)
        vm.estado.value.mensaje shouldBe "Solicitud cancelada"
    }

    "cancelar con error del servidor" {
        val (vm, sol) = crear()
        coEvery { sol.eliminar(10) } returns Result.failure(Exception("Solo se pueden cancelar solicitudes PENDIENTES"))
        vm.cancelar(pendiente)
        vm.estado.value.error shouldBe "Solo se pueden cancelar solicitudes PENDIENTES"
    }

    "editar descripción (UPDATE) valida y actualiza la lista" {
        val (vm, sol) = crear()
        vm.editarDescripcion(pendiente, "corta").shouldNotBeNull()
        val editada = pendiente.copy(descripcion = "Ahora además gotea agua")
        coEvery { sol.actualizarDescripcion(10, any()) } returns Result.success(editada)
        vm.editarDescripcion(pendiente, "Ahora además gotea agua").shouldBeNull()
        vm.estado.value.solicitudes.first { it.id == 10L }.descripcion shouldBe "Ahora además gotea agua"
        coEvery { sol.actualizarDescripcion(10, any()) } returns Result.failure(Exception("No editable"))
        vm.editarDescripcion(pendiente, "Otra descripción larga")
        vm.estado.value.error shouldBe "No editable"
    }
})

class NuevaSolicitudViewModelTest : ViewModelSpec({
    fun crear(puntos: List<PuntoVenta> = listOf(Fixtures.punto())): Triple<NuevaSolicitudViewModel, SolicitudRepository, ImageEncoder> {
        val sol = mockk<SolicitudRepository>()
        val cat = mockk<CatalogoRepository>()
        val enc = mockk<ImageEncoder>()
        coEvery { cat.puntos(5) } returns Result.success(puntos)
        coEvery { cat.equipos(puntoVentaId = 1) } returns Result.success(listOf(Fixtures.equipo()))
        return Triple(NuevaSolicitudViewModel(sol, cat, enc, 5), sol, enc)
    }

    "con un solo local se selecciona automáticamente y carga sus equipos" {
        val (vm) = crear()
        vm.estado.value.puntoId shouldBe 1L
        vm.estado.value.equiposDelPunto shouldHaveSize 1
        vm.tiposDisponibles.contains(TipoSolicitud.REVISION) shouldBe false
    }

    "error al cargar puntos" {
        val sol = mockk<SolicitudRepository>()
        val cat = mockk<CatalogoRepository>()
        coEvery { cat.puntos(5) } returns Result.failure(Exception("Sin conexión"))
        val vm = NuevaSolicitudViewModel(sol, cat, mockk(), 5)
        vm.estado.value.error shouldBe "Sin conexión"
    }

    "reparación exige equipo; equipo nuevo exige tipo de equipo" {
        val (vm) = crear(puntos = emptyList())
        vm.validarFormulario() shouldBe false
        vm.estado.value.errores.punto.shouldNotBeNull()
        vm.estado.value.errores.equipo.shouldNotBeNull()
        vm.seleccionarTipo(TipoSolicitud.EQUIPO_NUEVO)
        vm.estado.value.requiereEquipo shouldBe false
        vm.validarFormulario() shouldBe false
        vm.estado.value.errores.tipoEquipo.shouldNotBeNull()
        vm.estado.value.errores.equipo.shouldBeNull()
    }

    "enviar reparación con foto adjunta" {
        val (vm, sol, enc) = crear()
        coEvery { enc.codificar("content://foto") } returns "BASE64"
        val slot = slot<SolicitudRequest>()
        coEvery { sol.crear(capture(slot)) } returns Result.success(Fixtures.solicitud())
        vm.seleccionarEquipo(1)
        vm.onDescripcion("La vitrina no enfría desde ayer")
        vm.onFoto("content://foto")
        vm.enviar()
        vm.estado.value.creada shouldBe true
        slot.captured.equipoId shouldBe 1L
        slot.captured.fotoClienteBase64 shouldBe "BASE64"
        slot.captured.tipoEquipoSolicitado.shouldBeNull()
    }

    "enviar equipo nuevo sin foto" {
        val (vm, sol) = crear()
        val slot = slot<SolicitudRequest>()
        coEvery { sol.crear(capture(slot)) } returns Result.success(Fixtures.solicitud())
        vm.seleccionarTipo(TipoSolicitud.EQUIPO_NUEVO)
        vm.seleccionarTipoEquipo(TipoEquipo.CONSERVADORA)
        vm.onDescripcion("Necesito una conservadora para helados")
        vm.enviar()
        slot.captured.equipoId.shouldBeNull()
        slot.captured.tipoEquipoSolicitado shouldBe TipoEquipo.CONSERVADORA
    }

    "foto que no se puede procesar o error del servidor" {
        val (vm, sol, enc) = crear()
        coEvery { enc.codificar(any()) } returns null
        vm.seleccionarEquipo(1)
        vm.onDescripcion("La vitrina no enfría desde ayer")
        vm.onFoto("content://rota")
        vm.enviar()
        vm.estado.value.error shouldBe "No se pudo procesar la foto adjunta"
        vm.onFoto(null)
        coEvery { sol.crear(any()) } returns Result.failure(Exception("Ya existe una solicitud abierta"))
        vm.enviar()
        vm.estado.value.error shouldBe "Ya existe una solicitud abierta"
        vm.estado.value.enviando shouldBe false
    }

    "error al cargar equipos del punto" {
        val (vm) = crear()
        val cat = mockk<CatalogoRepository>()
        // nuevo VM con error en equipos
        coEvery { cat.puntos(5) } returns Result.success(listOf(Fixtures.punto(), Fixtures.punto(2)))
        coEvery { cat.equipos(puntoVentaId = 2) } returns Result.failure(Exception("Error equipos"))
        val vm2 = NuevaSolicitudViewModel(mockk(), cat, mockk(), 5)
        vm2.estado.value.puntoId.shouldBeNull()
        vm2.seleccionarPunto(2)
        vm2.estado.value.error shouldBe "Error equipos"
        vm.estado.value.error.shouldBeNull()
    }
})

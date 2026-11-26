package cl.duoc.tacticaldev.viewmodel

import cl.duoc.tacticaldev.Fixtures
import cl.duoc.tacticaldev.ViewModelSpec
import cl.duoc.tacticaldev.data.local.VisitaPendienteEntity
import cl.duoc.tacticaldev.data.model.*
import cl.duoc.tacticaldev.repository.*
import cl.duoc.tacticaldev.util.ImageEncoder
import cl.duoc.tacticaldev.util.LocationProvider
import cl.duoc.tacticaldev.util.UbicacionCapturada
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDateTime

class OperarioViewModelTest : ViewModelSpec({
    val asignada = Fixtures.solicitud(10, estado = EstadoSolicitud.ASIGNADA, operarioId = 2)
    val pendienteLocal = VisitaPendienteEntity.desde(
        VisitaRequest("u1", 10, 2, EstadoEquipo.OPERATIVO, null, "B64", 0.0, 0.0, null, false, "2026-11-18T10:00:00", null),
        "Minimarket 1", 1
    )

    fun crear(pendientes: List<VisitaPendienteEntity> = emptyList()): Triple<OperarioViewModel, VisitaRepository, MutableStateFlow<List<VisitaPendienteEntity>>> {
        val sol = mockk<SolicitudRepository>()
        val vis = mockk<VisitaRepository>()
        val flujo = MutableStateFlow(pendientes)
        every { vis.pendientes(2) } returns flujo
        coEvery { vis.solicitudesConPendiente(2) } returns pendientes.map { it.solicitudId }.toSet()
        coEvery { vis.sincronizar(2) } returns ResumenSincronizacion(0, 0, 0)
        coEvery { sol.listar(operarioId = 2, estado = EstadoSolicitud.ASIGNADA) } returns Result.success(listOf(asignada))
        coEvery { vis.listar(operarioId = 2) } returns Result.success(listOf(Fixtures.visita()))
        return Triple(OperarioViewModel(sol, vis, 2), vis, flujo)
    }

    "carga visitas asignadas e historial" {
        val (vm) = crear()
        vm.estado.value.asignadas.size shouldBe 1
        vm.estado.value.visitas.size shouldBe 1
        vm.estado.value.pendientesSync shouldBe 0
    }

    "si hay visitas guardadas sin conexión intenta enviarlas al cargar" {
        val (vm, vis) = crear(listOf(pendienteLocal))
        coVerify { vis.sincronizar(2) }
        vm.estado.value.pendientesSync shouldBe 1
        vm.estado.value.solicitudesConPendiente shouldBe setOf(10L)
    }

    "mensajes de sincronización" {
        val (vm, vis, flujo) = crear()
        vm.sincronizar()
        vm.estado.value.mensaje shouldBe "No hay visitas pendientes"
        coEvery { vis.sincronizar(2) } returns ResumenSincronizacion(0, 0, 2)
        vm.sincronizar()
        vm.estado.value.mensaje!! shouldContain "Sin conexión"
        coEvery { vis.sincronizar(2) } returns ResumenSincronizacion(1, 1, 0)
        vm.sincronizar()
        vm.estado.value.mensaje!! shouldContain "rechazada"
        coEvery { vis.sincronizar(2) } returns ResumenSincronizacion(2, 0, 0)
        vm.sincronizar()
        vm.estado.value.mensaje shouldBe "2 visita(s) sincronizada(s)"
        flujo.value = emptyList()
        vm.limpiarMensajes()
        vm.estado.value.mensaje.shouldBeNull()
    }
})

class VisitaViewModelTest : ViewModelSpec({
    val solicitud = Fixtures.solicitud(10, estado = EstadoSolicitud.ASIGNADA, operarioId = 2)
    val cerca = UbicacionCapturada(-33.5168, -70.7161, 12.0, false)   // ~14 m del local
    val lejos = UbicacionCapturada(-33.5000, -70.7000, 10.0, false)   // ~2 km

    class Deps {
        val sol = mockk<SolicitudRepository>()
        val vis = mockk<VisitaRepository>()
        val clima = mockk<ClimaRepository>()
        val gps = mockk<LocationProvider>()
        val enc = mockk<ImageEncoder>()
    }

    fun crear(d: Deps = Deps()): Pair<VisitaViewModel, Deps> {
        coEvery { d.sol.obtener(10) } returns Result.success(solicitud)
        coEvery { d.clima.climaActual(any(), any()) } returns Result.success(Clima(24.0, 40, "Despejado", false))
        coEvery { d.enc.codificar(any()) } returns "BASE64"
        val vm = VisitaViewModel(
            d.sol, d.vis, d.clima, d.gps, d.enc, operarioId = 2, solicitudId = 10,
            reloj = { LocalDateTime.of(2026, 11, 18, 12, 30, 0) }, generarId = { "uuid-fijo" }
        )
        return vm to d
    }

    "carga la solicitud a visitar" {
        val (vm) = crear()
        vm.estado.value.solicitud shouldBe solicitud
        vm.estado.value.cargando shouldBe false
    }

    "error al cargar la solicitud" {
        val d = Deps()
        coEvery { d.sol.obtener(10) } returns Result.failure(Exception("Solicitud no encontrada"))
        val vm = VisitaViewModel(d.sol, d.vis, d.clima, d.gps, d.enc, 2, 10)
        vm.estado.value.error shouldBe "Solicitud no encontrada"
    }

    "foto dentro del radio queda verificada y trae el clima (API externa)" {
        val (vm, d) = crear()
        coEvery { d.gps.obtenerUbicacionActual() } returns cerca
        vm.onFotoTomada("content://foto1")
        val c = vm.estado.value.captura.shouldNotBeNull()
        c.dentroDelRadio shouldBe true
        c.fechaCaptura shouldBe "2026-11-18T12:30:00"
        c.motivo shouldContain "verificada"
        vm.estado.value.clima?.temperatura shouldBe 24.0
        vm.estado.value.puedeEnviar shouldBe true
    }

    "foto lejos del local NO permite enviar (visita fantasma)" {
        val (vm, d) = crear()
        coEvery { d.gps.obtenerUbicacionActual() } returns lejos
        vm.onFotoTomada("content://foto2")
        vm.estado.value.captura!!.dentroDelRadio shouldBe false
        vm.estado.value.puedeEnviar shouldBe false
        vm.onEstadoEquipo(EstadoEquipo.OPERATIVO)
        vm.validarFormulario() shouldBe false
        vm.estado.value.evidenciaError!! shouldContain "del punto de venta"
    }

    "GPS simulado o con mala precisión no se acepta" {
        val (vm, d) = crear()
        coEvery { d.gps.obtenerUbicacionActual() } returns cerca.copy(esSimulada = true)
        vm.onFotoTomada("content://f")
        vm.estado.value.captura!!.motivo shouldContain "simulado"
        coEvery { d.gps.obtenerUbicacionActual() } returns cerca.copy(precisionMetros = 500.0)
        vm.onFotoTomada("content://f")
        vm.estado.value.captura!!.motivo shouldContain "débil"
    }

    "sin GPS se pide repetir la foto; sin clima no se bloquea" {
        val (vm, d) = crear()
        coEvery { d.gps.obtenerUbicacionActual() } returns null
        vm.onFotoTomada("content://f")
        vm.estado.value.captura.shouldBeNull()
        vm.estado.value.evidenciaError.shouldNotBeNull()
        coEvery { d.gps.obtenerUbicacionActual() } returns cerca
        coEvery { d.clima.climaActual(any(), any()) } returns Result.failure(Exception("sin red"))
        vm.onFotoTomada("content://f")
        vm.estado.value.climaNoDisponible shouldBe true
        vm.estado.value.puedeEnviar shouldBe true
    }

    "validaciones del formulario" {
        val (vm, d) = crear()
        vm.validarFormulario() shouldBe false
        vm.estado.value.estadoEquipoError.shouldNotBeNull()
        vm.estado.value.evidenciaError shouldBe "Debe tomar la foto de evidencia en el punto de venta"
        coEvery { d.gps.obtenerUbicacionActual() } returns cerca
        vm.onFotoTomada("content://f")
        vm.onEstadoEquipo(EstadoEquipo.CON_FALLA)
        vm.validarFormulario() shouldBe false
        vm.estado.value.observacionesError.shouldNotBeNull()
        vm.onObservaciones("Compresor no arranca, se lleva a taller")
        vm.validarFormulario() shouldBe true
    }

    "enviar arma la visita con la foto y el GPS ligados" {
        val (vm, d) = crear()
        coEvery { d.gps.obtenerUbicacionActual() } returns cerca
        val slot = slot<VisitaRequest>()
        coEvery { d.vis.registrar(capture(slot), "Minimarket 1") } returns
                Result.success(ResultadoRegistro.Enviada(Fixtures.visita()))
        vm.onFotoTomada("content://foto1")
        vm.onEstadoEquipo(EstadoEquipo.OPERATIVO)
        vm.onObservaciones("  ")
        vm.enviar()
        vm.estado.value.resultado.shouldBeInstanceOf<ResultadoRegistro.Enviada>()
        with(slot.captured) {
            idLocal shouldBe "uuid-fijo"
            latitud shouldBe cerca.latitud
            fotoBase64 shouldBe "BASE64"
            fechaCaptura shouldBe "2026-11-18T12:30:00"
            temperaturaAmbiente shouldBe 24.0
            observaciones.shouldBeNull()
        }
    }

    "sin conexión queda guardada; foto ilegible o rechazo muestran error" {
        val (vm, d) = crear()
        coEvery { d.gps.obtenerUbicacionActual() } returns cerca
        vm.onFotoTomada("content://foto1")
        vm.onEstadoEquipo(EstadoEquipo.OPERATIVO)

        coEvery { d.enc.codificar(any()) } returns null
        vm.enviar()
        vm.estado.value.error!! shouldContain "foto"

        coEvery { d.enc.codificar(any()) } returns "BASE64"
        coEvery { d.vis.registrar(any(), any()) } returns Result.failure(Exception("Asignada a otro operario"))
        vm.enviar()
        vm.estado.value.error shouldBe "Asignada a otro operario"

        coEvery { d.vis.registrar(any(), any()) } returns Result.success(ResultadoRegistro.GuardadaSinConexion)
        vm.enviar()
        vm.estado.value.resultado shouldBe ResultadoRegistro.GuardadaSinConexion
    }
})

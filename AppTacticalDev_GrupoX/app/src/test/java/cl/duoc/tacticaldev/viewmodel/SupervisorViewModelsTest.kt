package cl.duoc.tacticaldev.viewmodel

import cl.duoc.tacticaldev.Fixtures
import cl.duoc.tacticaldev.ViewModelSpec
import cl.duoc.tacticaldev.data.model.*
import cl.duoc.tacticaldev.repository.*
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.*

class SupervisorViewModelTest : ViewModelSpec({
    val tecnico = Fixtures.usuario(2, Rol.OPERARIO, TipoOperario.TECNICO)
    val vendedor = Fixtures.usuario(3, Rol.OPERARIO, TipoOperario.VENDEDOR)
    val inactivo = Fixtures.usuario(4, Rol.OPERARIO, TipoOperario.TRANSPORTISTA, activo = false)
    val reparacion = Fixtures.solicitud(10, TipoSolicitud.REPARACION)
    val nuevo = Fixtures.solicitud(11, TipoSolicitud.EQUIPO_NUEVO, equipo = null, tipoEquipoSolicitado = TipoEquipo.CONSERVADORA)
    val cerrada = Fixtures.solicitud(12, estado = EstadoSolicitud.VALIDADA)

    class Deps {
        val sol = mockk<SolicitudRepository>()
        val vis = mockk<VisitaRepository>()
        val usu = mockk<UsuarioRepository>()
        val cat = mockk<CatalogoRepository>()
    }

    fun crear(d: Deps = Deps()): Pair<SupervisorViewModel, Deps> {
        coEvery { d.sol.listar() } returns Result.success(listOf(reparacion, nuevo, cerrada))
        coEvery { d.vis.listar(estado = EstadoValidacion.PENDIENTE) } returns Result.success(listOf(Fixtures.visita(1)))
        coEvery { d.vis.listar(estado = EstadoValidacion.SOSPECHOSA) } returns
                Result.success(listOf(Fixtures.visita(2, EstadoValidacion.SOSPECHOSA, verificada = false)))
        coEvery { d.usu.listar(Rol.OPERARIO) } returns Result.success(listOf(tecnico, vendedor, inactivo))
        coEvery { d.cat.equipos(enBodega = true) } returns Result.success(listOf(
            Fixtures.equipo(5, TipoEquipo.CONSERVADORA, puntoId = null),
            Fixtures.equipo(6, TipoEquipo.VITRINA_FREEZER, puntoId = null),
            Fixtures.equipo(7, TipoEquipo.CONSERVADORA, EstadoEquipo.CON_FALLA, puntoId = null)
        ))
        return SupervisorViewModel(d.sol, d.vis, d.usu, d.cat) to d
    }

    "panel: indicadores y sospechosas primero" {
        val (vm) = crear()
        with(vm.estado.value) {
            solicitudes shouldHaveSize 3
            pendientes shouldBe 2
            sospechosas shouldBe 1
            visitasPorRevisar.first().estadoValidacion shouldBe EstadoValidacion.SOSPECHOSA
            error.shouldBeNull()
        }
    }

    "filtro por estado" {
        val (vm) = crear()
        vm.cambiarFiltro(EstadoSolicitud.VALIDADA)
        vm.estado.value.solicitudesFiltradas.map { it.id } shouldBe listOf(12L)
        vm.cambiarFiltro(null)
        vm.estado.value.solicitudesFiltradas shouldHaveSize 3
    }

    "solo operarios activos y compatibles con el tipo de solicitud" {
        val (vm) = crear()
        vm.operariosCompatibles(TipoSolicitud.REPARACION) shouldBe listOf(tecnico)
        vm.operariosCompatibles(TipoSolicitud.EQUIPO_NUEVO) shouldBe listOf(vendedor)
        vm.operariosCompatibles(TipoSolicitud.RETIRO) shouldBe listOf(vendedor)
        vm.operariosCompatibles(TipoSolicitud.REVISION) shouldBe listOf(tecnico, vendedor)
        vm.equiposCompatibles(nuevo).map { it.id } shouldBe listOf(5L)
    }

    "asignar valida datos y actualiza la lista" {
        val (vm, d) = crear()
        vm.asignar(reparacion, null, null)
        vm.estado.value.error shouldBe "Seleccione un operario"
        vm.asignar(nuevo, 3, null)
        vm.estado.value.error!! shouldContain "bodega"
        val asignada = reparacion.copy(estado = EstadoSolicitud.ASIGNADA, operarioId = 2, operarioNombre = "Tomás")
        coEvery { d.sol.asignar(10, 2, null) } returns Result.success(asignada)
        vm.asignar(reparacion, 2, null)
        vm.estado.value.solicitudes.first { it.id == 10L }.estado shouldBe EstadoSolicitud.ASIGNADA
        vm.estado.value.mensaje!! shouldContain "Tomás"
    }

    "asignar equipo nuevo recarga la bodega; error del servidor" {
        val (vm, d) = crear()
        coEvery { d.sol.asignar(11, 3, 5) } returns Result.success(nuevo.copy(estado = EstadoSolicitud.ASIGNADA))
        vm.asignar(nuevo, 3, 5)
        coVerify(exactly = 2) { d.cat.equipos(enBodega = true) }
        coEvery { d.sol.asignar(10, 3, null) } returns Result.failure(Exception("Un VENDEDOR no puede atender"))
        vm.asignar(reparacion, 3, null)
        vm.estado.value.error shouldBe "Un VENDEDOR no puede atender"
        vm.limpiarMensajes()
        vm.estado.value.error.shouldBeNull()
    }
})

class DetalleVisitaViewModelTest : ViewModelSpec({
    fun crear(v: Visita): Pair<DetalleVisitaViewModel, VisitaRepository> {
        val repo = mockk<VisitaRepository>()
        coEvery { repo.obtener(v.id) } returns Result.success(v.copy(fotoBase64 = "FOTO"))
        return DetalleVisitaViewModel(repo, v.id) to repo
    }

    "rechazar exige motivo" {
        val (vm) = crear(Fixtures.visita())
        vm.estado.value.puedeValidar shouldBe true
        vm.validar(false)
        vm.estado.value.comentarioError.shouldNotBeNull()
    }

    "aprobar una sospechosa exige justificación" {
        val (vm, repo) = crear(Fixtures.visita(validacion = EstadoValidacion.SOSPECHOSA, verificada = false))
        vm.validarComentario(true) shouldBe false
        vm.onComentario("x".repeat(301))
        vm.validarComentario(true) shouldBe false
        vm.onComentario("Local en galería comercial, GPS impreciso")
        coEvery { repo.validar(100, true, any()) } returns Result.success(Fixtures.visita(validacion = EstadoValidacion.VALIDADA))
        vm.validar(true)
        vm.estado.value.terminado shouldBe true
        vm.estado.value.visita?.fotoBase64 shouldBe "FOTO"
        vm.estado.value.puedeValidar shouldBe false
    }

    "aprobar una visita normal y error del servidor" {
        val (vm, repo) = crear(Fixtures.visita())
        coEvery { repo.validar(100, true, "") } returns Result.failure(Exception("La visita ya fue revisada"))
        vm.validar(true)
        vm.estado.value.error shouldBe "La visita ya fue revisada"
        vm.estado.value.procesando shouldBe false
    }

    "visita inexistente" {
        val repo = mockk<VisitaRepository>()
        coEvery { repo.obtener(1) } returns Result.failure(Exception("Visita 1 no encontrada"))
        DetalleVisitaViewModel(repo, 1).estado.value.error shouldBe "Visita 1 no encontrada"
    }
})

class HistorialViewModelTest : ViewModelSpec({
    "historial del equipo y error" {
        val cat = mockk<CatalogoRepository>()
        coEvery { cat.historial(1) } returns Result.success(listOf(Fixtures.visita(1), Fixtures.visita(2)))
        HistorialViewModel(cat, 1).estado.value.visitas shouldHaveSize 2
        coEvery { cat.historial(2) } returns Result.failure(Exception("Equipo 2 no encontrado"))
        HistorialViewModel(cat, 2).estado.value.error shouldBe "Equipo 2 no encontrado"
    }
})

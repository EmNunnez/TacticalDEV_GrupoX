package cl.duoc.tacticaldev

import cl.duoc.tacticaldev.data.local.VisitaPendienteEntity
import cl.duoc.tacticaldev.data.model.*
import cl.duoc.tacticaldev.repository.ResultadoRegistro
import cl.duoc.tacticaldev.repository.ResumenSincronizacion
import cl.duoc.tacticaldev.ui.navigation.Rutas
import cl.duoc.tacticaldev.util.UbicacionCapturada
import cl.duoc.tacticaldev.viewmodel.*
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/** Todos los modelos de la app cumplen igualdad por valor, copy() y tienen sus etiquetas. */
class ModelosTest : StringSpec({

    "modelos que se intercambian con el backend (data/model)" {
        listOf(
            Usuario::class, LoginRequest::class, UsuarioRequest::class, Sesion::class, ErrorResponse::class,
            PuntoVenta::class, PuntoVentaRequest::class, Equipo::class, EquipoRequest::class,
            Solicitud::class, SolicitudRequest::class, SolicitudUpdateRequest::class, AsignarRequest::class,
            Visita::class, VisitaRequest::class, ValidarRequest::class,
            ClimaResponse::class, ClimaActual::class, Clima::class
        ).forEach(ContratoModelos::verificarDataClass)
    }

    "modelos locales, del repositorio y utilidades" {
        listOf(
            VisitaPendienteEntity::class, UbicacionCapturada::class,
            ResumenSincronizacion::class, ResultadoRegistro.Enviada::class
        ).forEach(ContratoModelos::verificarDataClass)
        ContratoModelos.verificarObjeto(ResultadoRegistro.GuardadaSinConexion)
    }

    "estados de pantalla y formularios (viewmodel)" {
        listOf(
            EstadoSesion.Activa::class, LoginUiState::class, RegistroErrores::class, RegistroUiState::class,
            ClienteUiState::class, NuevaSolicitudErrores::class, NuevaSolicitudUiState::class,
            OperarioUiState::class, CapturaEvidencia::class, VisitaUiState::class,
            SupervisorUiState::class, DetalleVisitaUiState::class, HistorialUiState::class,
            PuntoForm::class, EquipoForm::class, UsuarioForm::class, GestionUiState::class
        ).forEach(ContratoModelos::verificarDataClass)
        ContratoModelos.verificarObjeto(EstadoSesion.Cargando)
        ContratoModelos.verificarObjeto(EstadoSesion.SinSesion)
    }

    "enums con sus etiquetas para la interfaz" {
        listOf(
            Rol::class, TipoOperario::class, TipoEquipo::class, EstadoEquipo::class,
            TipoSolicitud::class, EstadoSolicitud::class, EstadoValidacion::class
        ).forEach(ContratoModelos::verificarEnum)
        Rol.CLIENTE.etiqueta shouldBe "Cliente"
        EstadoSolicitud.COMPLETADA.etiqueta shouldBe "Por validar"
        EstadoValidacion.SOSPECHOSA.etiqueta shouldBe "Sospechosa"
    }

    "rutas de navegación (data object)" {
        Rutas::class.sealedSubclasses.forEach { ContratoModelos.verificarObjeto(it.objectInstance!!) }
        Rutas::class.sealedSubclasses.size shouldBe 13
    }
})

package cl.duoc.tacticaldev.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.tacticaldev.data.model.*
import cl.duoc.tacticaldev.repository.CatalogoRepository
import cl.duoc.tacticaldev.repository.SolicitudRepository
import cl.duoc.tacticaldev.repository.UsuarioRepository
import cl.duoc.tacticaldev.repository.VisitaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SupervisorUiState(
    val cargando: Boolean = false,
    val solicitudes: List<Solicitud> = emptyList(),
    val visitasPorRevisar: List<Visita> = emptyList(),
    val operarios: List<Usuario> = emptyList(),
    val equiposBodega: List<Equipo> = emptyList(),
    val filtro: EstadoSolicitud? = null,
    val procesando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null
) {
    val solicitudesFiltradas get() = if (filtro == null) solicitudes else solicitudes.filter { it.estado == filtro }
    val pendientes get() = solicitudes.count { it.estado == EstadoSolicitud.PENDIENTE }
    val sospechosas get() = visitasPorRevisar.count { it.estadoValidacion == EstadoValidacion.SOSPECHOSA }
}

class SupervisorViewModel(
    private val solicitudes: SolicitudRepository,
    private val visitas: VisitaRepository,
    private val usuarios: UsuarioRepository,
    private val catalogo: CatalogoRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(SupervisorUiState())
    val estado: StateFlow<SupervisorUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val rSol = solicitudes.listar()
            val rPend = visitas.listar(estado = EstadoValidacion.PENDIENTE)
            val rSosp = visitas.listar(estado = EstadoValidacion.SOSPECHOSA)
            val rOps = usuarios.listar(Rol.OPERARIO)
            val rBodega = catalogo.equipos(enBodega = true)
            val porRevisar = (rSosp.getOrDefault(emptyList()) + rPend.getOrDefault(emptyList()))
            _estado.update {
                it.copy(
                    cargando = false,
                    solicitudes = rSol.getOrDefault(it.solicitudes),
                    visitasPorRevisar = porRevisar,
                    operarios = rOps.getOrDefault(it.operarios),
                    equiposBodega = rBodega.getOrDefault(it.equiposBodega),
                    error = listOf(rSol, rPend, rOps).firstNotNullOfOrNull { r -> r.exceptionOrNull()?.message }
                )
            }
        }
    }

    fun cambiarFiltro(estado: EstadoSolicitud?) = _estado.update { it.copy(filtro = estado) }

    /** Misma regla que el backend: quién puede atender cada tipo de solicitud. */
    fun operariosCompatibles(tipo: TipoSolicitud): List<Usuario> = _estado.value.operarios.filter { u ->
        u.activo && when (tipo) {
            TipoSolicitud.REPARACION -> u.tipoOperario == TipoOperario.TECNICO
            TipoSolicitud.EQUIPO_NUEVO, TipoSolicitud.RETIRO ->
                u.tipoOperario == TipoOperario.VENDEDOR || u.tipoOperario == TipoOperario.TRANSPORTISTA
            TipoSolicitud.REVISION -> true
        }
    }

    /** Equipos de bodega que sirven para una solicitud de EQUIPO_NUEVO. */
    fun equiposCompatibles(solicitud: Solicitud): List<Equipo> = _estado.value.equiposBodega.filter {
        it.tipo == solicitud.tipoEquipoSolicitado && it.estado == EstadoEquipo.OPERATIVO
    }

    fun asignar(solicitud: Solicitud, operarioId: Long?, equipoId: Long?) {
        if (operarioId == null) {
            _estado.update { it.copy(error = "Seleccione un operario") }
            return
        }
        if (solicitud.tipo == TipoSolicitud.EQUIPO_NUEVO && equipoId == null) {
            _estado.update { it.copy(error = "Seleccione el equipo de bodega a instalar") }
            return
        }
        _estado.update { it.copy(procesando = true) }
        viewModelScope.launch {
            solicitudes.asignar(solicitud.id, operarioId, equipoId)
                .onSuccess { actualizada ->
                    _estado.update { s ->
                        s.copy(
                            procesando = false,
                            solicitudes = s.solicitudes.map { if (it.id == actualizada.id) actualizada else it },
                            mensaje = "Solicitud #${actualizada.id} asignada a ${actualizada.operarioNombre}"
                        )
                    }
                    if (solicitud.tipo == TipoSolicitud.EQUIPO_NUEVO) cargar()
                }
                .onFailure { e -> _estado.update { it.copy(procesando = false, error = e.message) } }
        }
    }

    fun limpiarMensajes() = _estado.update { it.copy(error = null, mensaje = null) }
}

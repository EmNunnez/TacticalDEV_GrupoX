package cl.duoc.tacticaldev.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.tacticaldev.data.model.EstadoSolicitud
import cl.duoc.tacticaldev.data.model.Solicitud
import cl.duoc.tacticaldev.data.model.Visita
import cl.duoc.tacticaldev.repository.SolicitudRepository
import cl.duoc.tacticaldev.repository.VisitaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OperarioUiState(
    val cargando: Boolean = false,
    val asignadas: List<Solicitud> = emptyList(),
    val visitas: List<Visita> = emptyList(),
    /** Visitas guardadas en el teléfono esperando internet. */
    val pendientesSync: Int = 0,
    val solicitudesConPendiente: Set<Long> = emptySet(),
    val sincronizando: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null
)

class OperarioViewModel(
    private val solicitudes: SolicitudRepository,
    private val visitas: VisitaRepository,
    private val operarioId: Long
) : ViewModel() {

    private val _estado = MutableStateFlow(OperarioUiState())
    val estado: StateFlow<OperarioUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            visitas.pendientes(operarioId).collect { lista ->
                _estado.update { s ->
                    s.copy(pendientesSync = lista.size, solicitudesConPendiente = lista.map { it.solicitudId }.toSet())
                }
            }
        }
        cargar()
    }

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            // Primero intenta subir lo que quedó guardado sin conexión
            if (visitas.solicitudesConPendiente(operarioId).isNotEmpty()) visitas.sincronizar(operarioId)

            val rAsignadas = solicitudes.listar(operarioId = operarioId, estado = EstadoSolicitud.ASIGNADA)
            val rVisitas = visitas.listar(operarioId = operarioId)
            _estado.update {
                it.copy(
                    cargando = false,
                    asignadas = rAsignadas.getOrDefault(it.asignadas),
                    visitas = rVisitas.getOrDefault(it.visitas),
                    error = rAsignadas.exceptionOrNull()?.message
                )
            }
        }
    }

    fun sincronizar() {
        if (_estado.value.sincronizando) return
        _estado.update { it.copy(sincronizando = true) }
        viewModelScope.launch {
            val r = visitas.sincronizar(operarioId)
            val mensaje = when {
                r.enviadas == 0 && r.rechazadas == 0 && r.pendientes == 0 -> "No hay visitas pendientes"
                r.pendientes > 0 -> "Sin conexión: ${r.pendientes} visita(s) siguen pendientes"
                r.rechazadas > 0 -> "${r.enviadas} enviada(s), ${r.rechazadas} rechazada(s) por el servidor"
                else -> "${r.enviadas} visita(s) sincronizada(s)"
            }
            _estado.update { it.copy(sincronizando = false, mensaje = mensaje) }
            if (r.enviadas > 0 || r.rechazadas > 0) cargar()
        }
    }

    fun limpiarMensajes() = _estado.update { it.copy(error = null, mensaje = null) }
}

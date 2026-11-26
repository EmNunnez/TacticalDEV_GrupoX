package cl.duoc.tacticaldev.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.tacticaldev.data.model.EstadoValidacion
import cl.duoc.tacticaldev.data.model.Visita
import cl.duoc.tacticaldev.repository.VisitaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetalleVisitaUiState(
    val cargando: Boolean = true,
    val visita: Visita? = null,
    val comentario: String = "",
    val comentarioError: String? = null,
    val procesando: Boolean = false,
    val terminado: Boolean = false,
    val error: String? = null
) {
    val puedeValidar get() = visita?.estadoValidacion == EstadoValidacion.PENDIENTE ||
            visita?.estadoValidacion == EstadoValidacion.SOSPECHOSA
}

class DetalleVisitaViewModel(
    private val visitas: VisitaRepository,
    private val visitaId: Long
) : ViewModel() {

    private val _estado = MutableStateFlow(DetalleVisitaUiState())
    val estado: StateFlow<DetalleVisitaUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            visitas.obtener(visitaId)
                .onSuccess { v -> _estado.update { it.copy(cargando = false, visita = v) } }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = e.message) } }
        }
    }

    fun onComentario(v: String) = _estado.update { it.copy(comentario = v, comentarioError = null) }

    /** Reglas: rechazar exige motivo; aprobar una visita SOSPECHOSA exige justificación. */
    fun validarComentario(aprobada: Boolean): Boolean {
        val s = _estado.value
        val sospechosa = s.visita?.estadoValidacion == EstadoValidacion.SOSPECHOSA
        val error = when {
            !aprobada && s.comentario.isBlank() -> "Indique el motivo del rechazo"
            aprobada && sospechosa && s.comentario.isBlank() -> "Justifique por qué aprueba una visita sospechosa"
            s.comentario.length > 300 -> "Máximo 300 caracteres"
            else -> null
        }
        _estado.update { it.copy(comentarioError = error) }
        return error == null
    }

    fun validar(aprobada: Boolean) {
        if (_estado.value.procesando || !validarComentario(aprobada)) return
        _estado.update { it.copy(procesando = true, error = null) }
        viewModelScope.launch {
            visitas.validar(visitaId, aprobada, _estado.value.comentario)
                .onSuccess { v ->
                    _estado.update { it.copy(procesando = false, terminado = true, visita = v.copy(fotoBase64 = it.visita?.fotoBase64)) }
                }
                .onFailure { e -> _estado.update { it.copy(procesando = false, error = e.message) } }
        }
    }
}

package cl.duoc.tacticaldev.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.tacticaldev.data.model.Visita
import cl.duoc.tacticaldev.repository.CatalogoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistorialUiState(
    val cargando: Boolean = true,
    val visitas: List<Visita> = emptyList(),
    val error: String? = null
)

/** Historial único y consultable de un activo (reemplaza WhatsApp/Excel/papel). */
class HistorialViewModel(
    private val catalogo: CatalogoRepository,
    private val equipoId: Long
) : ViewModel() {

    private val _estado = MutableStateFlow(HistorialUiState())
    val estado: StateFlow<HistorialUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            catalogo.historial(equipoId)
                .onSuccess { l -> _estado.update { it.copy(cargando = false, visitas = l) } }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = e.message) } }
        }
    }
}

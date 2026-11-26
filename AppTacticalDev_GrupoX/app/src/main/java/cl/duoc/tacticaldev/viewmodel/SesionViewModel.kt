package cl.duoc.tacticaldev.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.tacticaldev.data.model.Sesion
import cl.duoc.tacticaldev.repository.AuthRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface EstadoSesion {
    data object Cargando : EstadoSesion
    data object SinSesion : EstadoSesion
    data class Activa(val sesion: Sesion) : EstadoSesion
}

/** Compartido por toda la app: decide a qué pantalla de inicio va cada rol. */
class SesionViewModel(private val auth: AuthRepository) : ViewModel() {

    val estado: StateFlow<EstadoSesion> = auth.sesion
        .map { if (it == null) EstadoSesion.SinSesion else EstadoSesion.Activa(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, EstadoSesion.Cargando)

    fun cerrarSesion() {
        viewModelScope.launch { auth.cerrarSesion() }
    }
}

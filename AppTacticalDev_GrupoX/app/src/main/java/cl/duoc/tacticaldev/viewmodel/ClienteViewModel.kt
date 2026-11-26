package cl.duoc.tacticaldev.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.tacticaldev.data.model.Equipo
import cl.duoc.tacticaldev.data.model.EstadoSolicitud
import cl.duoc.tacticaldev.data.model.PuntoVenta
import cl.duoc.tacticaldev.data.model.Solicitud
import cl.duoc.tacticaldev.repository.CatalogoRepository
import cl.duoc.tacticaldev.repository.SolicitudRepository
import cl.duoc.tacticaldev.util.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClienteUiState(
    val cargando: Boolean = false,
    val solicitudes: List<Solicitud> = emptyList(),
    val puntos: List<PuntoVenta> = emptyList(),
    val equipos: List<Equipo> = emptyList(),
    val error: String? = null,
    val mensaje: String? = null
) {
    val abiertas get() = solicitudes.count { it.estado != EstadoSolicitud.VALIDADA }
}

class ClienteViewModel(
    private val solicitudes: SolicitudRepository,
    private val catalogo: CatalogoRepository,
    private val clienteId: Long
) : ViewModel() {

    private val _estado = MutableStateFlow(ClienteUiState())
    val estado: StateFlow<ClienteUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val rSolicitudes = solicitudes.listar(clienteId = clienteId)
            val rPuntos = catalogo.puntos(clienteId)
            val puntos = rPuntos.getOrDefault(emptyList())
            val equipos = puntos.flatMap { p -> catalogo.equipos(puntoVentaId = p.id).getOrDefault(emptyList()) }
            val error = (rSolicitudes.exceptionOrNull() ?: rPuntos.exceptionOrNull())?.message
            _estado.update {
                it.copy(
                    cargando = false,
                    solicitudes = rSolicitudes.getOrDefault(it.solicitudes),
                    puntos = puntos,
                    equipos = equipos,
                    error = error
                )
            }
        }
    }

    /** DELETE: solo solicitudes PENDIENTES. */
    fun cancelar(solicitud: Solicitud) {
        if (solicitud.estado != EstadoSolicitud.PENDIENTE) {
            _estado.update { it.copy(error = "Solo puede cancelar solicitudes pendientes") }
            return
        }
        viewModelScope.launch {
            solicitudes.eliminar(solicitud.id)
                .onSuccess {
                    _estado.update { s ->
                        s.copy(solicitudes = s.solicitudes.filterNot { it.id == solicitud.id }, mensaje = "Solicitud cancelada")
                    }
                }
                .onFailure { e -> _estado.update { it.copy(error = e.message) } }
        }
    }

    /** UPDATE: el cliente corrige la descripción mientras nadie la ha tomado. Devuelve el error o null. */
    fun editarDescripcion(solicitud: Solicitud, descripcion: String): String? {
        Validaciones.descripcion(descripcion)?.let { return it }
        viewModelScope.launch {
            solicitudes.actualizarDescripcion(solicitud.id, descripcion)
                .onSuccess { actualizada ->
                    _estado.update { s ->
                        s.copy(
                            solicitudes = s.solicitudes.map { if (it.id == actualizada.id) actualizada else it },
                            mensaje = "Solicitud actualizada"
                        )
                    }
                }
                .onFailure { e -> _estado.update { it.copy(error = e.message) } }
        }
        return null
    }

    fun limpiarMensajes() = _estado.update { it.copy(error = null, mensaje = null) }
}

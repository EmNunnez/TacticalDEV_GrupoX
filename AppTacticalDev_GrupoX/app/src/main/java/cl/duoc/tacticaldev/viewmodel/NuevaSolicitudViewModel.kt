package cl.duoc.tacticaldev.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.tacticaldev.data.model.*
import cl.duoc.tacticaldev.repository.CatalogoRepository
import cl.duoc.tacticaldev.repository.SolicitudRepository
import cl.duoc.tacticaldev.util.ImageEncoder
import cl.duoc.tacticaldev.util.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NuevaSolicitudErrores(
    val punto: String? = null,
    val equipo: String? = null,
    val tipoEquipo: String? = null,
    val descripcion: String? = null
) {
    val hayErrores get() = listOf(punto, equipo, tipoEquipo, descripcion).any { it != null }
}

data class NuevaSolicitudUiState(
    val cargando: Boolean = false,
    val puntos: List<PuntoVenta> = emptyList(),
    val equiposDelPunto: List<Equipo> = emptyList(),
    val tipo: TipoSolicitud = TipoSolicitud.REPARACION,
    val puntoId: Long? = null,
    val equipoId: Long? = null,
    val tipoEquipo: TipoEquipo? = null,
    val descripcion: String = "",
    /** Foto opcional de la falla (cámara o galería). */
    val fotoUri: String? = null,
    val errores: NuevaSolicitudErrores = NuevaSolicitudErrores(),
    val enviando: Boolean = false,
    val creada: Boolean = false,
    val error: String? = null
) {
    val requiereEquipo get() = tipo != TipoSolicitud.EQUIPO_NUEVO
}

class NuevaSolicitudViewModel(
    private val solicitudes: SolicitudRepository,
    private val catalogo: CatalogoRepository,
    private val encoder: ImageEncoder,
    private val clienteId: Long
) : ViewModel() {

    /** Lo que un cliente puede pedir (REVISION la programa el supervisor). */
    val tiposDisponibles = listOf(TipoSolicitud.REPARACION, TipoSolicitud.EQUIPO_NUEVO, TipoSolicitud.RETIRO)

    private val _estado = MutableStateFlow(NuevaSolicitudUiState())
    val estado: StateFlow<NuevaSolicitudUiState> = _estado.asStateFlow()

    init {
        _estado.update { it.copy(cargando = true) }
        viewModelScope.launch {
            catalogo.puntos(clienteId)
                .onSuccess { puntos ->
                    _estado.update { it.copy(cargando = false, puntos = puntos) }
                    if (puntos.size == 1) seleccionarPunto(puntos.first().id)
                }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = e.message) } }
        }
    }

    fun seleccionarTipo(tipo: TipoSolicitud) = _estado.update {
        it.copy(tipo = tipo, equipoId = null, tipoEquipo = null, errores = NuevaSolicitudErrores())
    }

    fun seleccionarPunto(id: Long) {
        _estado.update { it.copy(puntoId = id, equipoId = null, equiposDelPunto = emptyList(), errores = it.errores.copy(punto = null)) }
        viewModelScope.launch {
            catalogo.equipos(puntoVentaId = id)
                .onSuccess { lista -> _estado.update { it.copy(equiposDelPunto = lista) } }
                .onFailure { e -> _estado.update { it.copy(error = e.message) } }
        }
    }

    fun seleccionarEquipo(id: Long) = _estado.update { it.copy(equipoId = id, errores = it.errores.copy(equipo = null)) }
    fun seleccionarTipoEquipo(t: TipoEquipo) = _estado.update { it.copy(tipoEquipo = t, errores = it.errores.copy(tipoEquipo = null)) }
    fun onDescripcion(v: String) = _estado.update { it.copy(descripcion = v, errores = it.errores.copy(descripcion = null)) }
    fun onFoto(uri: String?) = _estado.update { it.copy(fotoUri = uri) }

    fun validarFormulario(): Boolean {
        val s = _estado.value
        val errores = NuevaSolicitudErrores(
            punto = if (s.puntoId == null) "Seleccione el punto de venta" else null,
            equipo = if (s.requiereEquipo && s.equipoId == null) "Seleccione el equipo" else null,
            tipoEquipo = if (!s.requiereEquipo && s.tipoEquipo == null) "Indique qué equipo necesita" else null,
            descripcion = Validaciones.descripcion(s.descripcion)
        )
        _estado.update { it.copy(errores = errores) }
        return !errores.hayErrores
    }

    fun enviar() {
        if (_estado.value.enviando || !validarFormulario()) return
        val s = _estado.value
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            val foto = s.fotoUri?.let { encoder.codificar(it) }
            if (s.fotoUri != null && foto == null) {
                _estado.update { it.copy(enviando = false, error = "No se pudo procesar la foto adjunta") }
                return@launch
            }
            val req = SolicitudRequest(
                tipo = s.tipo,
                descripcion = s.descripcion.trim(),
                clienteId = clienteId,
                puntoVentaId = s.puntoId!!,
                equipoId = if (s.requiereEquipo) s.equipoId else null,
                tipoEquipoSolicitado = if (s.requiereEquipo) null else s.tipoEquipo,
                fotoClienteBase64 = foto
            )
            solicitudes.crear(req)
                .onSuccess { _estado.update { it.copy(enviando = false, creada = true) } }
                .onFailure { e -> _estado.update { it.copy(enviando = false, error = e.message) } }
        }
    }
}

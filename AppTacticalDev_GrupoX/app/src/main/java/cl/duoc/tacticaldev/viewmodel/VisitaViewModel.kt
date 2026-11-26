package cl.duoc.tacticaldev.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.tacticaldev.data.model.Clima
import cl.duoc.tacticaldev.data.model.EstadoEquipo
import cl.duoc.tacticaldev.data.model.Solicitud
import cl.duoc.tacticaldev.data.model.VisitaRequest
import cl.duoc.tacticaldev.repository.ClimaRepository
import cl.duoc.tacticaldev.repository.ResultadoRegistro
import cl.duoc.tacticaldev.repository.SolicitudRepository
import cl.duoc.tacticaldev.repository.VisitaRepository
import cl.duoc.tacticaldev.util.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.UUID

/**
 * Evidencia de la visita: la foto y el GPS quedan LIGADOS porque la ubicación
 * se obtiene en el instante en que la cámara devuelve la foto. Si se repite la foto,
 * se repite el GPS.
 */
data class CapturaEvidencia(
    val fotoUri: String,
    val latitud: Double,
    val longitud: Double,
    val precisionMetros: Double?,
    val esSimulada: Boolean,
    val fechaCaptura: String,
    val distanciaMetros: Double,
    val dentroDelRadio: Boolean,
    val motivo: String
)

data class VisitaUiState(
    val cargando: Boolean = true,
    val solicitud: Solicitud? = null,
    val estadoEquipo: EstadoEquipo? = null,
    val observaciones: String = "",
    val captura: CapturaEvidencia? = null,
    val capturandoGps: Boolean = false,
    val clima: Clima? = null,
    val climaNoDisponible: Boolean = false,
    val estadoEquipoError: String? = null,
    val observacionesError: String? = null,
    val evidenciaError: String? = null,
    val enviando: Boolean = false,
    val resultado: ResultadoRegistro? = null,
    val error: String? = null
) {
    val puedeEnviar get() = captura?.dentroDelRadio == true && !enviando && !capturandoGps
}

class VisitaViewModel(
    private val solicitudes: SolicitudRepository,
    private val visitas: VisitaRepository,
    private val clima: ClimaRepository,
    private val gps: LocationProvider,
    private val encoder: ImageEncoder,
    private val operarioId: Long,
    private val solicitudId: Long,
    private val reloj: () -> LocalDateTime = { LocalDateTime.now() },
    private val generarId: () -> String = { UUID.randomUUID().toString() }
) : ViewModel() {

    private val _estado = MutableStateFlow(VisitaUiState())
    val estado: StateFlow<VisitaUiState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            solicitudes.obtener(solicitudId)
                .onSuccess { s -> _estado.update { it.copy(cargando = false, solicitud = s) } }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = e.message) } }
        }
    }

    fun onEstadoEquipo(e: EstadoEquipo) = _estado.update {
        it.copy(estadoEquipo = e, estadoEquipoError = null, observacionesError = null)
    }

    fun onObservaciones(v: String) = _estado.update { it.copy(observaciones = v, observacionesError = null) }

    /**
     * Se llama apenas la cámara devuelve la foto. En ese mismo momento se pide
     * el GPS y se verifica contra la ubicación del punto de venta.
     */
    fun onFotoTomada(uri: String) {
        val punto = _estado.value.solicitud?.puntoVenta ?: return
        val fecha = reloj()
        _estado.update { it.copy(capturandoGps = true, captura = null, evidenciaError = null, error = null) }
        viewModelScope.launch {
            val ub = gps.obtenerUbicacionActual()
            if (ub == null) {
                _estado.update {
                    it.copy(
                        capturandoGps = false,
                        evidenciaError = "No se pudo obtener el GPS. Active la ubicación y vuelva a tomar la foto"
                    )
                }
                return@launch
            }
            val distancia = GeoUtils.distanciaMetros(ub.latitud, ub.longitud, punto.latitud, punto.longitud)
            val (dentro, motivo) = when {
                ub.esSimulada -> false to "Se detectó GPS simulado. La visita no puede registrarse"
                (ub.precisionMetros ?: 0.0) > GeoUtils.PRECISION_MAXIMA_M ->
                    false to "Señal GPS débil (±${ub.precisionMetros!!.toInt()} m). Salga al exterior y repita la foto"
                distancia > punto.radioMetros ->
                    false to "Está a ${GeoUtils.formatearDistancia(distancia)} del punto de venta (máx. ${punto.radioMetros} m)"
                else -> true to "Ubicación verificada: a ${GeoUtils.formatearDistancia(distancia)} del punto de venta"
            }
            val captura = CapturaEvidencia(
                fotoUri = uri,
                latitud = ub.latitud,
                longitud = ub.longitud,
                precisionMetros = ub.precisionMetros,
                esSimulada = ub.esSimulada,
                fechaCaptura = Formatos.fechaIso(fecha),
                distanciaMetros = distancia,
                dentroDelRadio = dentro,
                motivo = motivo
            )
            _estado.update { it.copy(capturandoGps = false, captura = captura) }
            cargarClima(ub.latitud, ub.longitud)
        }
    }

    /** API externa: temperatura ambiente del lugar. Si falla no bloquea la visita. */
    private suspend fun cargarClima(lat: Double, lon: Double) {
        clima.climaActual(lat, lon)
            .onSuccess { c -> _estado.update { it.copy(clima = c, climaNoDisponible = false) } }
            .onFailure { _estado.update { it.copy(clima = null, climaNoDisponible = true) } }
    }

    fun validarFormulario(): Boolean {
        val s = _estado.value
        val estadoError = if (s.estadoEquipo == null) "Seleccione el estado en que queda el equipo" else null
        val obsError = Validaciones.observacionVisita(s.observaciones, s.estadoEquipo == EstadoEquipo.CON_FALLA)
        val evidenciaError = when {
            s.captura == null -> "Debe tomar la foto de evidencia en el punto de venta"
            !s.captura.dentroDelRadio -> s.captura.motivo
            else -> null
        }
        _estado.update {
            it.copy(estadoEquipoError = estadoError, observacionesError = obsError, evidenciaError = evidenciaError)
        }
        return estadoError == null && obsError == null && evidenciaError == null
    }

    fun enviar() {
        if (_estado.value.enviando || !validarFormulario()) return
        val s = _estado.value
        val solicitud = s.solicitud ?: return
        val captura = s.captura ?: return
        _estado.update { it.copy(enviando = true, error = null) }
        viewModelScope.launch {
            val foto = encoder.codificar(captura.fotoUri)
            if (foto == null) {
                _estado.update { it.copy(enviando = false, error = "No se pudo procesar la foto. Tómela nuevamente") }
                return@launch
            }
            val req = VisitaRequest(
                idLocal = generarId(),
                solicitudId = solicitud.id,
                operarioId = operarioId,
                estadoEquipo = s.estadoEquipo!!,
                observaciones = s.observaciones.trim().ifBlank { null },
                fotoBase64 = foto,
                latitud = captura.latitud,
                longitud = captura.longitud,
                precisionMetros = captura.precisionMetros,
                ubicacionSimulada = captura.esSimulada,
                fechaCaptura = captura.fechaCaptura,
                temperaturaAmbiente = s.clima?.temperatura
            )
            visitas.registrar(req, solicitud.puntoVenta.nombre)
                .onSuccess { r -> _estado.update { it.copy(enviando = false, resultado = r) } }
                .onFailure { e -> _estado.update { it.copy(enviando = false, error = e.message) } }
        }
    }
}

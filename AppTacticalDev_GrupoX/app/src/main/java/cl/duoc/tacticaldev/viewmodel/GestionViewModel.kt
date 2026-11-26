package cl.duoc.tacticaldev.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.tacticaldev.data.model.*
import cl.duoc.tacticaldev.repository.CatalogoRepository
import cl.duoc.tacticaldev.repository.SolicitudRepository
import cl.duoc.tacticaldev.repository.UsuarioRepository
import cl.duoc.tacticaldev.util.LocationProvider
import cl.duoc.tacticaldev.util.UbicacionCapturada
import cl.duoc.tacticaldev.util.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------- Formularios (lo que escribe el supervisor) ----------

data class PuntoForm(
    val nombre: String = "",
    val direccion: String = "",
    val comuna: String = "",
    val latitud: String = "",
    val longitud: String = "",
    val radio: String = "100",
    val clienteId: Long? = null
)

data class EquipoForm(
    val codigo: String = "",
    val tipo: TipoEquipo? = null,
    val marca: String = "",
    val modelo: String = "",
    val estado: EstadoEquipo = EstadoEquipo.OPERATIVO,
    val puntoVentaId: Long? = null
)

data class UsuarioForm(
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val password: String = "",
    val rol: Rol? = null,
    val tipoOperario: TipoOperario? = null,
    val activo: Boolean = true
)

data class GestionUiState(
    val cargando: Boolean = false,
    val puntos: List<PuntoVenta> = emptyList(),
    val equipos: List<Equipo> = emptyList(),
    val usuarios: List<Usuario> = emptyList(),
    val erroresForm: Map<String, String> = emptyMap(),
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val ubicacionActual: UbicacionCapturada? = null,
    val obteniendoUbicacion: Boolean = false,
    val error: String? = null,
    val mensaje: String? = null,
    // Formularios en el ViewModel: sobreviven a la rotación de pantalla
    val puntoForm: PuntoForm = PuntoForm(),
    val equipoForm: EquipoForm = EquipoForm(),
    val usuarioForm: UsuarioForm = UsuarioForm(),
    /** Evita sobreescribir lo que el usuario ya editó al recargar las listas. */
    val formCargado: String? = null
) {
    val clientes get() = usuarios.filter { it.rol == Rol.CLIENTE }
    val equiposConFalla get() = equipos.count { it.estado == EstadoEquipo.CON_FALLA }
}

/** CRUD de datos maestros que administra el Supervisor. */
class GestionViewModel(
    private val catalogo: CatalogoRepository,
    private val usuarios: UsuarioRepository,
    private val solicitudes: SolicitudRepository,
    private val gps: LocationProvider
) : ViewModel() {

    private val _estado = MutableStateFlow(GestionUiState())
    val estado: StateFlow<GestionUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val rP = catalogo.puntos()
            val rE = catalogo.equipos()
            val rU = usuarios.listar()
            _estado.update {
                it.copy(
                    cargando = false,
                    puntos = rP.getOrDefault(it.puntos),
                    equipos = rE.getOrDefault(it.equipos),
                    usuarios = rU.getOrDefault(it.usuarios),
                    error = listOf(rP, rE, rU).firstNotNullOfOrNull { r -> r.exceptionOrNull()?.message }
                )
            }
        }
    }

    // ---------- estado de los formularios ----------
    fun onPuntoForm(f: PuntoForm) = _estado.update { it.copy(puntoForm = f) }
    fun onEquipoForm(f: EquipoForm) = _estado.update { it.copy(equipoForm = f) }
    fun onUsuarioForm(f: UsuarioForm) = _estado.update { it.copy(usuarioForm = f) }

    /** Rellena el formulario con la entidad a editar (una sola vez). */
    fun prepararPunto(id: Long) {
        if (_estado.value.formCargado == "p$id") return
        val p = _estado.value.puntos.firstOrNull { it.id == id } ?: return
        _estado.update {
            it.copy(
                formCargado = "p$id",
                puntoForm = PuntoForm(p.nombre, p.direccion, p.comuna, p.latitud.toString(), p.longitud.toString(),
                    p.radioMetros.toString(), p.clienteId)
            )
        }
    }

    fun prepararEquipo(id: Long) {
        if (_estado.value.formCargado == "e$id") return
        val e = _estado.value.equipos.firstOrNull { it.id == id } ?: return
        _estado.update {
            it.copy(formCargado = "e$id", equipoForm = EquipoForm(e.codigo, e.tipo, e.marca, e.modelo, e.estado, e.puntoVentaId))
        }
    }

    fun prepararUsuario(id: Long) {
        if (_estado.value.formCargado == "u$id") return
        val u = _estado.value.usuarios.firstOrNull { it.id == id } ?: return
        _estado.update {
            it.copy(
                formCargado = "u$id",
                usuarioForm = UsuarioForm(u.nombre, u.email, u.telefono.orEmpty(), "", u.rol, u.tipoOperario, u.activo)
            )
        }
    }

    fun limpiarMensajes() = _estado.update { it.copy(error = null, mensaje = null) }
    fun reiniciarFormulario() = _estado.update { it.copy(erroresForm = emptyMap(), guardado = false, error = null) }

    // ================= PUNTOS DE VENTA =================

    fun validarPunto(f: PuntoForm): Map<String, String> = buildMap {
        Validaciones.obligatorio(f.nombre, "El nombre")?.let { put("nombre", it) }
        Validaciones.obligatorio(f.direccion, "La dirección")?.let { put("direccion", it) }
        Validaciones.obligatorio(f.comuna, "La comuna")?.let { put("comuna", it) }
        Validaciones.latitud(f.latitud)?.let { put("latitud", it) }
        Validaciones.longitud(f.longitud)?.let { put("longitud", it) }
        Validaciones.radio(f.radio)?.let { put("radio", it) }
        if (f.clienteId == null) put("cliente", "Seleccione el cliente dueño del local")
    }

    fun guardarPunto(id: Long?, f: PuntoForm) {
        if (!aceptarFormulario(validarPunto(f))) return
        val req = PuntoVentaRequest(
            nombre = f.nombre.trim(), direccion = f.direccion.trim(), comuna = f.comuna.trim(),
            latitud = f.latitud.replace(',', '.').toDouble(), longitud = f.longitud.replace(',', '.').toDouble(),
            radioMetros = f.radio.toInt(), clienteId = f.clienteId!!
        )
        guardar { if (id == null) catalogo.crearPunto(req) else catalogo.actualizarPunto(id, req) }
    }

    fun eliminarPunto(id: Long) = eliminar("Punto de venta eliminado") { catalogo.eliminarPunto(id) }

    /** Para registrar un local nuevo parado en la puerta: toma el GPS actual. */
    fun usarUbicacionActual() {
        _estado.update { it.copy(obteniendoUbicacion = true) }
        viewModelScope.launch {
            val ub = gps.obtenerUbicacionActual()
            _estado.update {
                it.copy(
                    obteniendoUbicacion = false,
                    ubicacionActual = ub,
                    puntoForm = if (ub == null) it.puntoForm
                    else it.puntoForm.copy(latitud = ub.latitud.toString(), longitud = ub.longitud.toString()),
                    error = if (ub == null) "No se pudo obtener la ubicación. Revise el permiso de GPS" else null
                )
            }
        }
    }

    // ================= EQUIPOS =================

    fun validarEquipo(f: EquipoForm): Map<String, String> = buildMap {
        Validaciones.codigoEquipo(f.codigo)?.let { put("codigo", it) }
        if (f.tipo == null) put("tipo", "Seleccione el tipo de equipo")
        Validaciones.obligatorio(f.marca, "La marca")?.let { put("marca", it) }
        Validaciones.obligatorio(f.modelo, "El modelo")?.let { put("modelo", it) }
        if (f.estado == EstadoEquipo.RETIRADO && f.puntoVentaId != null) {
            put("punto", "Un equipo retirado debe quedar en bodega")
        }
    }

    fun guardarEquipo(id: Long?, f: EquipoForm) {
        if (!aceptarFormulario(validarEquipo(f))) return
        val req = EquipoRequest(
            codigo = f.codigo.trim().uppercase(), tipo = f.tipo!!, marca = f.marca.trim(),
            modelo = f.modelo.trim(), estado = f.estado, puntoVentaId = f.puntoVentaId
        )
        guardar { if (id == null) catalogo.crearEquipo(req) else catalogo.actualizarEquipo(id, req) }
    }

    fun eliminarEquipo(id: Long) = eliminar("Equipo eliminado") { catalogo.eliminarEquipo(id) }

    /** Visita periódica: el supervisor crea una solicitud REVISION para un equipo instalado. */
    fun programarRevision(equipo: Equipo) {
        val punto = _estado.value.puntos.firstOrNull { it.id == equipo.puntoVentaId }
        if (punto == null) {
            _estado.update { it.copy(error = "Solo se pueden revisar equipos instalados en un punto de venta") }
            return
        }
        viewModelScope.launch {
            solicitudes.crear(
                SolicitudRequest(
                    tipo = TipoSolicitud.REVISION,
                    descripcion = "Revisión periódica programada del equipo ${equipo.codigo}",
                    clienteId = punto.clienteId,
                    puntoVentaId = punto.id,
                    equipoId = equipo.id,
                    tipoEquipoSolicitado = null,
                    fotoClienteBase64 = null
                )
            )
                .onSuccess { s -> _estado.update { it.copy(mensaje = "Revisión #${s.id} creada. Asígnela en Solicitudes") } }
                .onFailure { e -> _estado.update { it.copy(error = e.message) } }
        }
    }

    // ================= USUARIOS =================

    fun validarUsuario(f: UsuarioForm, esNuevo: Boolean): Map<String, String> = buildMap {
        Validaciones.nombre(f.nombre)?.let { put("nombre", it) }
        Validaciones.email(f.email)?.let { put("email", it) }
        Validaciones.telefono(f.telefono)?.let { put("telefono", it) }
        (if (esNuevo) Validaciones.password(f.password) else Validaciones.passwordOpcional(f.password))
            ?.let { put("password", it) }
        if (f.rol == null) put("rol", "Seleccione el rol")
        if (f.rol == Rol.OPERARIO && f.tipoOperario == null) put("tipoOperario", "Seleccione el tipo de operario")
    }

    fun guardarUsuario(id: Long?, f: UsuarioForm) {
        if (!aceptarFormulario(validarUsuario(f, id == null))) return
        val req = UsuarioRequest(
            nombre = f.nombre.trim(), email = f.email.trim(), password = f.password.ifEmpty { null },
            telefono = f.telefono.trim().ifBlank { null }, rol = f.rol!!,
            tipoOperario = if (f.rol == Rol.OPERARIO) f.tipoOperario else null, activo = f.activo
        )
        guardar { if (id == null) usuarios.crear(req) else usuarios.actualizar(id, req) }
    }

    fun eliminarUsuario(id: Long) = eliminar("Usuario eliminado") { usuarios.eliminar(id) }

    // ================= helpers =================

    private fun aceptarFormulario(errores: Map<String, String>): Boolean {
        _estado.update { it.copy(erroresForm = errores) }
        return errores.isEmpty()
    }

    private fun guardar(operacion: suspend () -> Result<*>) {
        if (_estado.value.guardando) return
        _estado.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            operacion()
                .onSuccess {
                    _estado.update { it.copy(guardando = false, guardado = true, mensaje = "Guardado correctamente") }
                    cargar()
                }
                .onFailure { e -> _estado.update { it.copy(guardando = false, error = e.message) } }
        }
    }

    private fun eliminar(mensajeOk: String, operacion: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            operacion()
                .onSuccess {
                    _estado.update { it.copy(mensaje = mensajeOk) }
                    cargar()
                }
                .onFailure { e -> _estado.update { it.copy(error = e.message) } }
        }
    }
}

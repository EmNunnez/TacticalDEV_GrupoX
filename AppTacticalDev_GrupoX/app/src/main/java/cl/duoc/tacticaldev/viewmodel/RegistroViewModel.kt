package cl.duoc.tacticaldev.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.tacticaldev.repository.AuthRepository
import cl.duoc.tacticaldev.util.Validaciones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistroErrores(
    val nombre: String? = null,
    val email: String? = null,
    val telefono: String? = null,
    val password: String? = null,
    val confirmar: String? = null,
    val terminos: String? = null
) {
    val hayErrores get() = listOf(nombre, email, telefono, password, confirmar, terminos).any { it != null }
}

data class RegistroUiState(
    val nombre: String = "",
    val email: String = "",
    val telefono: String = "",
    val password: String = "",
    val confirmar: String = "",
    val aceptaTerminos: Boolean = false,
    val errores: RegistroErrores = RegistroErrores(),
    val cargando: Boolean = false,
    val error: String? = null
)

/** Registro de CLIENTES (dueños de minimarket). Operarios y supervisores los crea el supervisor. */
class RegistroViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _estado = MutableStateFlow(RegistroUiState())
    val estado: StateFlow<RegistroUiState> = _estado.asStateFlow()

    fun onNombre(v: String) = _estado.update { it.copy(nombre = v, errores = it.errores.copy(nombre = null)) }
    fun onEmail(v: String) = _estado.update { it.copy(email = v, errores = it.errores.copy(email = null)) }
    fun onTelefono(v: String) = _estado.update { it.copy(telefono = v, errores = it.errores.copy(telefono = null)) }
    fun onPassword(v: String) = _estado.update { it.copy(password = v, errores = it.errores.copy(password = null)) }
    fun onConfirmar(v: String) = _estado.update { it.copy(confirmar = v, errores = it.errores.copy(confirmar = null)) }
    fun onTerminos(v: Boolean) = _estado.update { it.copy(aceptaTerminos = v, errores = it.errores.copy(terminos = null)) }

    fun validarFormulario(): Boolean {
        val s = _estado.value
        val errores = RegistroErrores(
            nombre = Validaciones.nombre(s.nombre),
            email = Validaciones.email(s.email),
            telefono = Validaciones.telefono(s.telefono),
            password = Validaciones.password(s.password),
            confirmar = Validaciones.confirmarPassword(s.password, s.confirmar),
            terminos = if (s.aceptaTerminos) null else "Debe aceptar el uso de cámara y ubicación"
        )
        _estado.update { it.copy(errores = errores) }
        return !errores.hayErrores
    }

    /** Crea la cuenta y luego inicia sesión automáticamente. */
    fun registrar() {
        if (_estado.value.cargando || !validarFormulario()) return
        val s = _estado.value
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val resultado = auth.registrarCliente(s.nombre, s.email, s.telefono, s.password)
                .mapCatching { auth.login(s.email, s.password).getOrThrow() }
            resultado
                .onSuccess { _estado.update { it.copy(cargando = false) } }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = e.message) } }
        }
    }
}

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

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val cargando: Boolean = false,
    val error: String? = null,
    val exito: Boolean = false
)

class LoginViewModel(private val auth: AuthRepository) : ViewModel() {

    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    fun onEmailChange(valor: String) = _estado.update { it.copy(email = valor, emailError = null, error = null) }

    fun onPasswordChange(valor: String) = _estado.update { it.copy(password = valor, passwordError = null, error = null) }

    fun validar(): Boolean {
        val s = _estado.value
        val emailError = Validaciones.email(s.email)
        val passError = if (s.password.isBlank()) "La contraseña es obligatoria" else null
        _estado.update { it.copy(emailError = emailError, passwordError = passError) }
        return emailError == null && passError == null
    }

    fun login() {
        if (_estado.value.cargando || !validar()) return
        val s = _estado.value
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            auth.login(s.email, s.password)
                .onSuccess { _estado.update { it.copy(cargando = false, exito = true) } }
                .onFailure { e -> _estado.update { it.copy(cargando = false, error = e.message) } }
        }
    }
}

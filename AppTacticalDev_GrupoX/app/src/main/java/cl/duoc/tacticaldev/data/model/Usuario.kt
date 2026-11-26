package cl.duoc.tacticaldev.data.model

data class Usuario(
    val id: Long,
    val nombre: String,
    val email: String,
    val telefono: String?,
    val rol: Rol,
    val tipoOperario: TipoOperario?,
    val activo: Boolean
)

data class LoginRequest(val email: String, val password: String)

data class UsuarioRequest(
    val nombre: String,
    val email: String,
    val password: String?,
    val telefono: String?,
    val rol: Rol,
    val tipoOperario: TipoOperario?,
    val activo: Boolean? = true
)

/** Lo que se guarda en DataStore al iniciar sesión. */
data class Sesion(
    val id: Long,
    val nombre: String,
    val email: String,
    val rol: Rol,
    val tipoOperario: TipoOperario?
)

/** Formato de error que devuelven los microservicios: {"mensaje": "..."} */
data class ErrorResponse(val mensaje: String?)

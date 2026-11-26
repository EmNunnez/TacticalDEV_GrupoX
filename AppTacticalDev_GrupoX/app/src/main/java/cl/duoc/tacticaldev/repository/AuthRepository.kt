package cl.duoc.tacticaldev.repository

import cl.duoc.tacticaldev.data.local.SesionStore
import cl.duoc.tacticaldev.data.model.LoginRequest
import cl.duoc.tacticaldev.data.model.Rol
import cl.duoc.tacticaldev.data.model.Sesion
import cl.duoc.tacticaldev.data.model.Usuario
import cl.duoc.tacticaldev.data.model.UsuarioRequest
import cl.duoc.tacticaldev.data.remote.ApiUsuarios
import cl.duoc.tacticaldev.util.safeCall
import kotlinx.coroutines.flow.Flow

class AuthRepository(
    private val api: ApiUsuarios,
    private val sesionStore: SesionStore
) {
    val sesion: Flow<Sesion?> = sesionStore.sesion

    suspend fun login(email: String, password: String): Result<Usuario> =
        safeCall { api.login(LoginRequest(email.trim(), password)) }
            .onSuccess { sesionStore.guardar(it) }

    /** Auto-registro: siempre como CLIENTE. */
    suspend fun registrarCliente(nombre: String, email: String, telefono: String, password: String): Result<Usuario> =
        safeCall {
            api.registro(
                UsuarioRequest(
                    nombre = nombre.trim(),
                    email = email.trim(),
                    password = password,
                    telefono = telefono.trim().ifBlank { null },
                    rol = Rol.CLIENTE,
                    tipoOperario = null
                )
            )
        }

    suspend fun cerrarSesion() = sesionStore.cerrar()
}

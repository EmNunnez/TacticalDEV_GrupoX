package cl.duoc.tacticaldev.repository

import cl.duoc.tacticaldev.data.model.Rol
import cl.duoc.tacticaldev.data.model.Usuario
import cl.duoc.tacticaldev.data.model.UsuarioRequest
import cl.duoc.tacticaldev.data.remote.ApiUsuarios
import cl.duoc.tacticaldev.util.safeCall
import cl.duoc.tacticaldev.util.safeCallSinCuerpo

class UsuarioRepository(private val api: ApiUsuarios) {
    suspend fun listar(rol: Rol? = null): Result<List<Usuario>> = safeCall { api.listar(rol) }
    suspend fun crear(req: UsuarioRequest): Result<Usuario> = safeCall { api.crear(req) }
    suspend fun actualizar(id: Long, req: UsuarioRequest): Result<Usuario> = safeCall { api.actualizar(id, req) }
    suspend fun eliminar(id: Long): Result<Unit> = safeCallSinCuerpo { api.eliminar(id) }
}

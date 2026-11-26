package cl.duoc.tacticaldev.data.remote

import cl.duoc.tacticaldev.data.model.LoginRequest
import cl.duoc.tacticaldev.data.model.Rol
import cl.duoc.tacticaldev.data.model.Usuario
import cl.duoc.tacticaldev.data.model.UsuarioRequest
import retrofit2.Response
import retrofit2.http.*

/** Endpoints de usuarios y login del backend en AWS (puerto 8080). */
interface ApiUsuarios {

    @POST("api/auth/login")
    suspend fun login(@Body req: LoginRequest): Usuario

    @POST("api/auth/registro")
    suspend fun registro(@Body req: UsuarioRequest): Usuario

    @GET("api/usuarios")
    suspend fun listar(@Query("rol") rol: Rol? = null): List<Usuario>

    @GET("api/usuarios/{id}")
    suspend fun obtener(@Path("id") id: Long): Usuario

    @POST("api/usuarios")
    suspend fun crear(@Body req: UsuarioRequest): Usuario

    @PUT("api/usuarios/{id}")
    suspend fun actualizar(@Path("id") id: Long, @Body req: UsuarioRequest): Usuario

    @DELETE("api/usuarios/{id}")
    suspend fun eliminar(@Path("id") id: Long): Response<Unit>
}

package cl.duoc.tacticaldev.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cl.duoc.tacticaldev.data.model.Rol
import cl.duoc.tacticaldev.data.model.Sesion
import cl.duoc.tacticaldev.data.model.TipoOperario
import cl.duoc.tacticaldev.data.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Abstracción para poder simular la sesión en las pruebas unitarias. */
interface SesionStore {
    val sesion: Flow<Sesion?>
    suspend fun guardar(usuario: Usuario)
    suspend fun cerrar()
}

private val Context.dataStore by preferencesDataStore(name = "sesion")

/** Sesión persistente con DataStore Preferences (la app recuerda al usuario). */
class DataStoreSesionStore(private val context: Context) : SesionStore {

    private object Keys {
        val ID = longPreferencesKey("id")
        val NOMBRE = stringPreferencesKey("nombre")
        val EMAIL = stringPreferencesKey("email")
        val ROL = stringPreferencesKey("rol")
        val TIPO = stringPreferencesKey("tipo_operario")
    }

    override val sesion: Flow<Sesion?> = context.dataStore.data.map { p ->
        val id = p[Keys.ID] ?: return@map null
        val rol = p[Keys.ROL]?.let { runCatching { Rol.valueOf(it) }.getOrNull() } ?: return@map null
        Sesion(
            id = id,
            nombre = p[Keys.NOMBRE].orEmpty(),
            email = p[Keys.EMAIL].orEmpty(),
            rol = rol,
            tipoOperario = p[Keys.TIPO]?.let { runCatching { TipoOperario.valueOf(it) }.getOrNull() }
        )
    }

    override suspend fun guardar(usuario: Usuario) {
        context.dataStore.edit { p ->
            p[Keys.ID] = usuario.id
            p[Keys.NOMBRE] = usuario.nombre
            p[Keys.EMAIL] = usuario.email
            p[Keys.ROL] = usuario.rol.name
            if (usuario.tipoOperario != null) p[Keys.TIPO] = usuario.tipoOperario.name else p.remove(Keys.TIPO)
        }
    }

    override suspend fun cerrar() {
        context.dataStore.edit { it.clear() }
    }
}

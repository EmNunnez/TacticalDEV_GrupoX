package cl.duoc.tacticaldev

import android.content.Context
import cl.duoc.tacticaldev.data.local.AppDatabase
import cl.duoc.tacticaldev.data.local.DataStoreSesionStore
import cl.duoc.tacticaldev.data.remote.RetrofitInstance
import cl.duoc.tacticaldev.repository.*
import cl.duoc.tacticaldev.util.AndroidImageEncoder
import cl.duoc.tacticaldev.util.AndroidLocationProvider
import cl.duoc.tacticaldev.util.ImageEncoder
import cl.duoc.tacticaldev.util.LocationProvider

/** Inyección de dependencias manual: un único lugar donde se crean los repositorios. */
class AppContainer(context: Context) {
    private val db = AppDatabase.get(context)

    val authRepository = AuthRepository(RetrofitInstance.apiUsuarios, DataStoreSesionStore(context))
    val usuarioRepository = UsuarioRepository(RetrofitInstance.apiUsuarios)
    val catalogoRepository = CatalogoRepository(RetrofitInstance.apiOperaciones)
    val solicitudRepository = SolicitudRepository(RetrofitInstance.apiOperaciones)
    val visitaRepository = VisitaRepository(RetrofitInstance.apiOperaciones, db.visitaPendienteDao())
    val climaRepository = ClimaRepository(RetrofitInstance.apiClima)

    val locationProvider: LocationProvider = AndroidLocationProvider(context)
    val imageEncoder: ImageEncoder = AndroidImageEncoder(context)
}

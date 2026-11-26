package cl.duoc.tacticaldev.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** GPS real con FusedLocationProvider (Google Play Services). */
class AndroidLocationProvider(private val context: Context) : LocationProvider {

    private val cliente = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun obtenerUbicacionActual(): UbicacionCapturada? {
        val permiso = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permiso != PackageManager.PERMISSION_GRANTED) return null

        // Se pide una ubicación NUEVA (no la última guardada) para ligarla a la foto
        val token = CancellationTokenSource()
        val loc: Location? = withTimeoutOrNull(15_000) {
            cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token).await()
        }
        if (loc == null) {
            token.cancel()
            return null
        }
        return UbicacionCapturada(
            latitud = loc.latitude,
            longitud = loc.longitude,
            precisionMetros = if (loc.hasAccuracy()) loc.accuracy.toDouble() else null,
            esSimulada = esSimulada(loc)
        )
    }

    @Suppress("DEPRECATION")
    private fun esSimulada(loc: Location): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) loc.isMock else loc.isFromMockProvider
}

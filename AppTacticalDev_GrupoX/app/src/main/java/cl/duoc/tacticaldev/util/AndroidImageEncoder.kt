package cl.duoc.tacticaldev.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Reduce la foto a máximo 1024 px y la comprime en JPEG para que viaje liviana
 * (~150 KB) al microservicio y quepa en Room cuando no hay conexión.
 */
class AndroidImageEncoder(private val context: Context) : ImageEncoder {

    override suspend fun codificar(uri: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val u = Uri.parse(uri)
            val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(u)?.use { BitmapFactory.decodeStream(it, null, opciones) }

            var escala = 1
            while (opciones.outWidth / escala > LADO_MAXIMO * 2 || opciones.outHeight / escala > LADO_MAXIMO * 2) escala *= 2

            val bitmap = context.contentResolver.openInputStream(u)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = escala })
            } ?: return@runCatching null

            val factor = minOf(1f, LADO_MAXIMO.toFloat() / maxOf(bitmap.width, bitmap.height))
            val reducida = if (factor < 1f)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * factor).toInt(), (bitmap.height * factor).toInt(), true)
            else bitmap

            val salida = ByteArrayOutputStream()
            reducida.compress(Bitmap.CompressFormat.JPEG, CALIDAD, salida)
            Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    private companion object {
        const val LADO_MAXIMO = 1024
        const val CALIDAD = 70
    }
}

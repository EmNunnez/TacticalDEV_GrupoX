package cl.duoc.tacticaldev.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import java.io.File

object ImageUtils {

    /** Crea el archivo temporal donde la cámara guardará la foto (FileProvider). */
    fun crearUriTemporal(context: Context): Uri {
        val dir = File(context.cacheDir, "fotos").apply { mkdirs() }
        val archivo = File(dir, "foto_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    }

    fun base64AImagen(base64: String?): ImageBitmap? {
        if (base64.isNullOrBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
}

package ar.com.fiado.readings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class PreparedPhoto(val preview: Bitmap?, val jpeg: ByteArray)
interface PhotoPreparation { suspend fun prepare(uri: String): PreparedPhoto }
class AndroidPhotoPreparer(private val context: Context) : PhotoPreparation {
 override suspend fun prepare(uri: String): PreparedPhoto = withContext(Dispatchers.IO) {
  // ImageDecoder applies EXIF orientation. Set target dimensions from the header before decoding.
  var bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, Uri.parse(uri))) { decoder, info, _ ->
   require(info.mimeType in listOf("image/jpeg", "image/png", "image/webp")) { "Elegí una foto JPEG, PNG o WebP." }
   val width = info.size.width
   val height = info.size.height
   require(width > 0 && height > 0)
   val scale = minOf(1.0, 2400.0 / max(width, height))
   decoder.setTargetSize(max(1, (width * scale).roundToInt()), max(1, (height * scale).roundToInt()))
   decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
   decoder.setOnPartialImageListener { false }
  }
  if (bitmap.hasAlpha()) {
   val opaque = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
   Canvas(opaque).apply { drawColor(Color.WHITE); drawBitmap(bitmap, 0f, 0f, null) }
   bitmap.recycle()
   bitmap = opaque
  }
  // Bound work and prefer readable quality before reducing dimensions further.
  repeat(5) {
   for (quality in listOf(90, 80, 70)) {
    val output = ByteArrayOutputStream()
    check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output))
    if (output.size() <= ReadingJson.MAX_IMAGE_BYTES) return@withContext PreparedPhoto(bitmap, output.toByteArray())
   }
   val smaller = Bitmap.createScaledBitmap(bitmap, max(1, bitmap.width * 3 / 4), max(1, bitmap.height * 3 / 4), true)
   if (smaller !== bitmap) bitmap.recycle()
   bitmap = smaller
  }
  bitmap.recycle()
  throw ReadingFailure("No se pudo reducir la foto al tamaño permitido. Elegí otra imagen.")
 }
}

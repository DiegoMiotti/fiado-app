package ar.com.fiado

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import ar.com.fiado.readings.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReadingDeviceTest {
 private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
 @Test fun configurationIsEncryptedPersistsAndCanBeDeleted() {
  val directory = File(context.cacheDir, "isolated-credential-test").apply { mkdirs() }
  val isolated = object : ContextWrapper(context) { override fun getNoBackupFilesDir(): File = directory }
  val store = KeystoreConnectionStore(isolated)
  try {
   store.write(Connection(token = "fictitious-device-test-token"))
   val stored = File(directory, "reading-connection").readBytes()
   assertFalse(String(stored, Charsets.ISO_8859_1).contains("fictitious-device-test-token"))
   assertEquals("fictitious-device-test-token", KeystoreConnectionStore(isolated).read().token)
   store.delete()
   assertEquals("", store.read().token)
  } finally { store.delete(); directory.delete() }
 }
 @Test fun photoRespectsExifOrientationAndSizeLimits() = runBlocking {
  val file = File(context.cacheDir, "fictitious-exif-test.jpg")
  val bitmap = Bitmap.createBitmap(3000, 1500, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
  try {
   file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
   ExifInterface(file.absolutePath).apply {
    setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
    saveAttributes()
   }
   val prepared = AndroidPhotoPreparer(context).prepare(android.net.Uri.fromFile(file).toString())
   val preview = requireNotNull(prepared.preview)
   assertEquals(1200, preview.width)
   assertEquals(2400, preview.height)
   assertTrue(prepared.jpeg.size <= ReadingJson.MAX_IMAGE_BYTES)
   preview.recycle()
  } finally { bitmap.recycle(); file.delete() }
 }
}

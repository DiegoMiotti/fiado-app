package ar.com.fiado.readings

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

internal object CameraPhotos {
 fun create(context: Context): Uri {
  val directory = File(context.cacheDir, "camera").apply { check(isDirectory || mkdirs()) }
  // Recover abandoned captures without touching a camera currently open.
  directory.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 86_400_000L }
   ?.forEach { it.delete() }
  val file = File.createTempFile("capture-", ".jpg", directory)
  return try { FileProvider.getUriForFile(context, "${context.packageName}.photos", file) }
  catch (error: Exception) { file.delete(); throw error }
 }
 fun discard(context: Context, uri: Uri) {
  if (uri.scheme == "content" && uri.authority == "${context.packageName}.photos" &&
   uri.pathSegments.firstOrNull() == "camera") {
   runCatching { context.contentResolver.delete(uri, null, null) }
  }
 }
}

package ar.com.fiado.readings

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.SocketTimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ReadingFailure(val notice: String) : Exception(notice)
interface ReadingGateway { suspend fun read(connection: Connection, jpeg: ByteArray): Reading }

class HttpReadingGateway : ReadingGateway {
 override suspend fun read(connection: Connection, jpeg: ByteArray): Reading = withContext(Dispatchers.IO) {
  if (connection.token.isBlank()) throw ReadingFailure("Ingresá el token del teléfono en Conexión.")
  if (jpeg.size > ReadingJson.MAX_IMAGE_BYTES) throw ReadingFailure("La foto supera el tamaño permitido.")
  val body = ReadingJson.request(Base64.encodeToString(jpeg, Base64.NO_WRAP))
  val http = URL(backendUrl(connection.url) + "/api/v1/readings").openConnection() as HttpURLConnection
  suspendCancellableCoroutine { continuation ->
   continuation.invokeOnCancellation { http.disconnect() }
   try {
    http.apply {
     requestMethod = "POST"
     connectTimeout = 65_000
     readTimeout = 65_000
     instanceFollowRedirects = false
     doOutput = true
     setFixedLengthStreamingMode(body.size)
     setRequestProperty("Content-Type", "application/json")
     setRequestProperty("Accept", "application/json")
     setRequestProperty("Authorization", "Bearer ${connection.token}")
    }
    http.outputStream.use { it.write(body) }
    val status = http.responseCode
    val stream = if (status in 200..299) http.inputStream else http.errorStream
    val bytes = stream?.use { it.readNBytes(2_000_001) } ?: byteArrayOf()
    if (bytes.size > 2_000_000) throw ReadingFailure("El backend devolvió una respuesta demasiado grande.")
    val raw = String(bytes, Charsets.UTF_8)
    if (status !in 200..299) throw ReadingFailure(ReadingJson.error(status, raw))
    val result = try { ReadingJson.parse(raw) } catch (_: Exception) {
     throw ReadingFailure("La respuesta del backend no es válida. El cobro actual se conserva.")
    }
    if (continuation.isActive) continuation.resume(result)
   } catch (error: Exception) {
    val failure = when (error) {
     is ReadingFailure -> error
     is SocketTimeoutException -> ReadingFailure("Se agotó el tiempo de espera. La consulta puede haber consumido cuota.")
     else -> ReadingFailure("No se pudo conectar al backend. Revisá la conexión; no se reintentó la consulta.")
    }
    if (continuation.isActive) continuation.resumeWithException(failure)
   } finally { http.disconnect() }
  }
 }
}

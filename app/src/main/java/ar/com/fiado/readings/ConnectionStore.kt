package ar.com.fiado.readings

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.json.JSONObject
import java.net.URI
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

const val DEFAULT_BACKEND = "https://fiado-backend.vercel.app"
data class Connection(val url: String = DEFAULT_BACKEND, val token: String = "") {
 override fun toString() = "Connection(url=$url, token=[oculto])"
}
interface ConnectionStorage {
 fun read(): Connection
 fun write(connection: Connection)
 fun delete()
}
fun backendUrl(raw: String): String {
 val uri = URI(raw.trim())
 require(uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.rawUserInfo == null &&
  uri.rawQuery == null && uri.rawFragment == null && uri.path.orEmpty() in listOf("", "/")) {
  "Ingresá una URL HTTPS sin ruta, usuario ni parámetros."
 }
 return uri.toASCIIString().trimEnd('/')
}

/** Both URL and token stay encrypted in noBackupFilesDir; no credential enters SavedStateHandle. */
class KeystoreConnectionStore(context: Context) : ConnectionStorage {
 private val file = AtomicFile(File(context.noBackupFilesDir, "reading-connection"))
 private val alias = "fiado.reading.connection.v1"
 private fun key(): SecretKey {
  val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
  (store.getKey(alias, null) as? SecretKey)?.let { return it }
  return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
   init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
  }.generateKey()
 }
 @Synchronized override fun read(): Connection {
  if (!file.baseFile.exists()) return Connection()
  val bytes = file.readFully()
  require(bytes.size > 12)
  val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
   init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
  }
  val json = JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
  return Connection(backendUrl(json.getString("url")), json.getString("token"))
 }
 @Synchronized override fun write(connection: Connection) {
  val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
  val plain = JSONObject().put("url", backendUrl(connection.url)).put("token", connection.token)
   .toString().toByteArray(Charsets.UTF_8)
  val encrypted = cipher.iv + cipher.doFinal(plain)
  val output = file.startWrite()
  try { output.write(encrypted); file.finishWrite(output) }
  catch (error: Exception) { file.failWrite(output); throw error }
 }
 @Synchronized override fun delete() { file.delete() }
}

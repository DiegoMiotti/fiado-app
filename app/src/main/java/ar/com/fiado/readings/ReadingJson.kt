package ar.com.fiado.readings

import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

object ReadingJson {
 const val MAX_IMAGE_BYTES = 2_800_000
 const val MAX_BODY_BYTES = 4_000_000
 const val MAX_AUDIO_BYTES = 2_000_000
 fun audioRequest(base64: String): ByteArray = JSONObject()
  .put("audio", JSONObject().put("mimeType", "audio/mp4").put("base64", base64))
  .put("clients", JSONArray()).toString().toByteArray(Charsets.UTF_8).also {
   require(it.size <= MAX_BODY_BYTES) { "El audio supera el tamaño permitido." }
  }
 fun request(base64: String): ByteArray = JSONObject()
  .put("image", JSONObject().put("mimeType", "image/jpeg").put("base64", base64))
  .put("clients", JSONArray()).toString().toByteArray(Charsets.UTF_8).also {
   require(it.size <= MAX_BODY_BYTES) { "La foto supera el tamaño permitido." }
  }
 private fun JSONObject.nullableString(key: String): String? =
  if (isNull(key)) null else getString(key)
 private fun warnings(array: JSONArray): List<String> = (0 until array.length()).map { array.getString(it) }
 private fun number(value: Any?): BigDecimal? {
  if (value == null || value == JSONObject.NULL) return null
  require(value is Number) { "Importe inválido." }
  return value.toString().toBigDecimal().also { require(it.stripTrailingZeros().scale() <= 0) }
 }
 fun parse(raw: String): Reading {
  val root = JSONObject(raw)
  require(root.getString("moneda") == "ARS" && root.getString("unidad") == "pesos_enteros")
  require(root.getBoolean("requiere_confirmacion"))
  val clients = root.getJSONArray("clientes")
  require(clients.length() in 1..300)
  return Reading(root.getString("request_id"), (0 until clients.length()).map { index ->
   val client = clients.getJSONObject(index)
   val operations = client.getJSONArray("operaciones")
   val match = client.getJSONObject("coincidencia")
   val candidates = match.getJSONArray("candidatos")
   val status = match.getString("estado")
   require(status in listOf("coincidencia_exacta", "ambiguo", "sin_coincidencia"))
   ClientReading(client.nullableString("cliente").orEmpty(), (0 until operations.length()).map { i ->
    val operation = operations.getJSONObject(i)
    val type = MovementType.entries.single { it.wire == operation.getString("tipo") }
    val amount = number(operation.opt("importe"))
    require(amount == null || amount > BigDecimal.ZERO)
    Movement(type, operation.nullableString("referencia"), operation.nullableString("detalle"),
     amount?.setScale(0)?.toPlainString().orEmpty(), operation.getBoolean("requiere_revision"))
   }, warnings(client.getJSONArray("advertencias")),
    client.getBoolean("requiere_revision") || client.getBoolean("cliente_requiere_revision"), status,
    (0 until candidates.length()).map { i -> candidates.getJSONObject(i).let { Candidate(it.getString("id"), it.getString("name")) } },
    number(client.opt("total_calculado")))
  }, warnings(root.getJSONArray("advertencias")))
 }
 fun error(status: Int, body: String): String {
  val code = runCatching { JSONObject(body).getJSONObject("error").getString("code") }.getOrNull()
  return when (code) {
   "UNAUTHORIZED" -> "El token del teléfono no es válido. Reemplazalo en Conexión."
   "RATE_LIMIT" -> "Se alcanzó el límite de consultas. Intentá más tarde."
   "MONTHLY_LIMIT" -> "Se alcanzó la cuota mensual."
   "AI_TIMEOUT" -> "La IA tardó demasiado. La consulta puede haber consumido cuota."
   "AI_AUTH_FAILED" -> "El backend tiene una clave del proveedor rechazada. Revisá Vercel."
   "AI_INSUFFICIENT_BALANCE" -> "El proveedor de IA no tiene saldo disponible."
   "AI_REQUEST_REJECTED" -> "El proveedor rechazó el modelo o los parámetros del backend."
   "AUDIO_NOT_CONFIGURED" -> "Configurá el modelo de audio y su clave en el backend antes de enviar grabaciones."
   "AUDIO_TOO_LARGE" -> "El audio es demasiado grande. Grabá una cuenta más corta."
   else -> when (status) {
    400 -> "El backend rechazó la solicitud."
    401 -> "El token del teléfono no es válido. Reemplazalo en Conexión."
    413 -> "El archivo es demasiado grande. Usá una foto o grabación más pequeña."
    429 -> "Se alcanzó el límite de consultas."
    502, 503 -> "El backend, el proveedor o Redis no están disponibles."
    504 -> "La lectura agotó el tiempo de espera. La consulta puede haber consumido cuota."
    else -> "No se pudo completar la lectura (HTTP $status)."
   }
  }
 }
}

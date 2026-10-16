package ar.com.fiado

import ar.com.fiado.readings.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

internal fun movement(type: MovementType, amount: String, included: Boolean = true) = Movement(type, null, null, amount, false, included)
internal fun client(vararg movements: Movement) = ClientReading("Leo", movements.toList(), emptyList(), false, "sin_coincidencia", emptyList(), null)
internal val reviewContact = ReviewContact("Leonardo", "23456789", "23456789")
internal val sampleReading = """
 {"request_id":"fake-request","moneda":"ARS","unidad":"pesos_enteros","requiere_confirmacion":true,
 "clientes":[{"cliente":"Leo","cliente_requiere_revision":false,
 "coincidencia":{"estado":"coincidencia_exacta","candidatos":[{"id":"local","name":"Leonardo"}]},
 "operaciones":[{"tipo":"cargo","referencia":null,"detalle":null,"importe":13000,"requiere_revision":false},
 {"tipo":"pago","referencia":"martes","detalle":null,"importe":2000,"requiere_revision":true}],
 "total_calculado":11000,"requiere_revision":true,"advertencias":["Revisar letra"]}],"advertencias":[]}
 """.trimIndent()

class ReadingRulesTest {
 @Test fun parsesNullsCandidatesAndReviewFlags() {
  val reading = ReadingJson.parse(sampleReading)
  assertEquals("fake-request", reading.requestId)
  val block = reading.clients.single()
  assertTrue(block.needsReview)
  assertNull(block.movements.first().reference)
  assertEquals("local", block.candidates.single().id)
  assertEquals(BigDecimal("11000"), ReviewRules.total(block))
  val nulls = ReadingJson.parse(sampleReading.replace("\"importe\":13000", "\"importe\":null")
   .replace("\"cliente\":\"Leo\"", "\"cliente\":null").replace("\"total_calculado\":11000", "\"total_calculado\":null"))
  assertEquals("", nulls.clients.single().name)
  assertNull(nulls.clients.single().serverTotal)
  assertNull(ReviewRules.total(nulls.clients.single()))
 }
 @Test fun rejectsFractionalNegativeUnknownTypeAndWrongUnits() {
  listOf(sampleReading.replace("\"importe\":13000", "\"importe\":13.5"),
   sampleReading.replace("\"importe\":13000", "\"importe\":-1"),
   sampleReading.replace("\"tipo\":\"cargo\"", "\"tipo\":\"otro\""),
   sampleReading.replace("pesos_enteros", "centavos"),
   sampleReading.replace("\"requiere_confirmacion\":true", "\"requiere_confirmacion\":false")).forEach {
   assertTrue(runCatching { ReadingJson.parse(it) }.isFailure)
  }
 }
 @Test fun calculatesBalancesChargesMinusPaymentsExactlyAndExcludesDuplicates() {
  val block = client(movement(MovementType.SALDO, "10000"), movement(MovementType.CARGO, "4200"),
   movement(MovementType.PAGO, "3000"), movement(MovementType.CARGO, "11200", false))
  assertEquals(BigDecimal("11200"), ReviewRules.total(block))
  assertFalse(ReviewRules.canUseComplicated(block))
  val application = ReviewRules.application(block, reviewContact, false, true)!!
  assertEquals("11200", application.total)
  assertNull(application.previous)
 }
 @Test fun requiresNullCorrectionContactAndExplicitConfirmation() {
  val block = client(movement(MovementType.CARGO, ""))
  assertNull(ReviewRules.application(block, reviewContact, false, true))
  val fixed = client(movement(MovementType.CARGO, "5000"))
  assertNull(ReviewRules.application(fixed, reviewContact, false, false))
  assertNull(ReviewRules.application(fixed, null, false, true))
  assertNull(ReviewRules.application(fixed, reviewContact.copy(phone = "123"), false, true))
  assertNull(ReviewRules.application(fixed.copy(name = ""), reviewContact, false, true))
 }
 @Test fun blocksZeroNegativeAndSimpleOverflow() {
  listOf("100", "200").forEach { payment ->
   assertNull(ReviewRules.application(client(movement(MovementType.CARGO, "100"), movement(MovementType.PAGO, payment)), reviewContact, false, true))
  }
  val large = client(movement(MovementType.SALDO, "999999999"), movement(MovementType.CARGO, "1"))
  assertNull(ReviewRules.application(large, reviewContact, false, true))
  assertEquals("1000000000", ReviewRules.application(large, reviewContact, true, true)!!.total)
 }
 @Test fun warnsMultipleBalancesAndRestrictsComplicatedToFaithfulBreakdown() {
  val block = client(movement(MovementType.SALDO, "10"), movement(MovementType.SALDO, "20"), movement(MovementType.CARGO, "30"))
  assertTrue(ReviewRules.multipleBalances(block))
  assertFalse(ReviewRules.canUseComplicated(block))
  assertNull(ReviewRules.application(block, reviewContact, true, true))
  assertFalse(ReviewRules.canUseComplicated(client(movement(MovementType.CARGO, "30"))))
 }
 @Test fun errorsDistinguishPhoneTokenFromProviderCredential() {
  assertTrue(ReadingJson.error(401, "{}").contains("token del teléfono"))
  assertTrue(ReadingJson.error(502, """{"error":{"code":"AI_AUTH_FAILED"}}""").contains("proveedor"))
  mapOf(400 to "solicitud", 413 to "grande", 429 to "límite", 503 to "Redis", 504 to "cuota").forEach { (status, word) ->
   assertTrue(ReadingJson.error(status, "invalid-json").contains(word))
  }
 }
 @Test fun requestContainsImageAndEmptyClientsOnlyAndEnforcesBodySize() {
  val request = org.json.JSONObject(String(ReadingJson.request("YWJj")))
  assertFalse(request.has("text"))
  assertEquals(0, request.getJSONArray("clients").length())
  assertEquals("YWJj", request.getJSONObject("image").getString("base64"))
  assertTrue(runCatching { ReadingJson.request("a".repeat(ReadingJson.MAX_BODY_BYTES)) }.isFailure)
 }
 @Test fun validatesHttpsOriginOnly() {
  assertEquals(DEFAULT_BACKEND, backendUrl("$DEFAULT_BACKEND/"))
  listOf("http://example.com", "https://user:secret@example.com", "https://example.com?token=a", "https://example.com/api", "https://example.com#x").forEach {
   assertTrue(runCatching { backendUrl(it) }.isFailure)
  }
 }
}

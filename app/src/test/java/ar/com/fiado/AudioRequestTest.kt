package ar.com.fiado

import ar.com.fiado.readings.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AudioRequestTest {
 @Test fun payloadContainsOriginalAudioAndNoTranscriptionOrContacts() {
  val json = JSONObject(String(ReadingJson.audioRequest("AAAA"), Charsets.UTF_8))
  assertEquals(setOf("audio", "clients"), json.keys().asSequence().toSet())
  assertEquals("audio/mp4", json.getJSONObject("audio").getString("mimeType"))
  assertEquals("AAAA", json.getJSONObject("audio").getString("base64"))
  assertEquals(0, json.getJSONArray("clients").length())
 }
 @Test fun audioDetailsCannotBreakMessageBlock() {
  val account = client(movement(MovementType.CARGO, "4000").copy(detail = "cerveza\n``` detalle"))
  val reading = Reading("audio", listOf(account.copy(serverTotal = ReviewRules.total(account))), emptyList())
  val message = AutomaticMessageRules.draft(reading, 0, true, reviewContact)!!.message
  assertEquals(2, Regex("```").findAll(message).count())
  assertTrue(message.contains("4.000  cerveza detalle"))
 }
}

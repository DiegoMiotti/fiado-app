package ar.com.fiado
import org.junit.Assert.*
import org.junit.Test
import java.time.*
import java.net.URLDecoder

class MessageRulesTest {
 private val clock = Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))
 @Test fun greetingBoundaries() {
  mapOf("05:59" to "Buenas noches", "06:00" to "Buenos días", "11:59" to "Buenos días",
   "12:00" to "Buenas tardes", "19:59" to "Buenas tardes", "20:00" to "Buenas noches",
   "00:00" to "Buenas noches").forEach { (time, expected) ->
    assertEquals(expected, MessageRules.greeting(LocalTime.parse(time)))
  }
 }
 @Test fun exactMessageAndDecimals() {
  assertEquals("Buenos días, el fiado de hoy es $1.500,00.", MessageRules.message("1500", clock))
  assertEquals("Buenos días, el fiado de hoy es $1.500,50.", MessageRules.message("1500,5", clock))
  assertEquals(MessageRules.message("1500,5", clock), MessageRules.message("1500.50", clock))
  assertNotNull(MessageRules.amount("999999999,99"))
  assertNotNull(MessageRules.amount("0,01"))
 }
 @Test fun invalidAmounts() {
  listOf("", "0", "-1", "1.500", "1,2.3", "NaN", "1000000000", " 20", "1e3").forEach {
   assertNull(it, MessageRules.amount(it))
  }
 }
 @Test fun requiresExplicitValidInternationalPhone() {
  assertEquals("16502530000", MessageRules.internationalPhone("+1 (650) 253-0000"))
  listOf("16502530000", "01112345678", "+123", "+1ABC6502530000", "+16502530000 ext 9").forEach {
   assertNull(it, MessageRules.internationalPhone(it))
  }
 }
 @Test fun urlRoundTripPreservesText() {
  val message = MessageRules.message("1500", clock)!!
  val url = MessageRules.whatsappUrl("+16502530000", message)
  assertTrue(url.startsWith("https://wa.me/16502530000?text="))
  assertEquals(message, URLDecoder.decode(url.substringAfter("?text="), "UTF-8"))
  assertFalse(url.contains(" "))
 }
 @Test fun usesDeviceTimeZone() {
  assertTrue(MessageRules.message("1", clock.withZone(ZoneId.of("Asia/Tokyo")))!!.startsWith("Buenas noches"))
 }
}

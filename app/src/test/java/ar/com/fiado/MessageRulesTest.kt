package ar.com.fiado

import org.junit.Assert.*
import org.junit.Test
import java.time.*
import java.net.URLDecoder

class MessageRulesTest {
 private val clock = Clock.fixed(Instant.parse("2026-09-05T12:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))
 @Test fun greetingBoundaries() {
  mapOf("05:59" to "Hola buenas noches", "06:00" to "Hola buen día", "11:59" to "Hola buen día",
   "12:00" to "Hola buenas tardes", "19:59" to "Hola buenas tardes", "20:00" to "Hola buenas noches",
   "00:00" to "Hola buenas noches").forEach { (time, expected) ->
    assertEquals(expected, MessageRules.greeting(LocalTime.parse(time)))
  }
 }
 @Test fun exactMessageAndDecimals() {
  assertEquals("Hola buen día, lo de hoy es: $1.500,00", MessageRules.message("1500", clock))
  assertEquals("Hola buen día, lo de hoy es: $1.500,50", MessageRules.message("1500,5", clock))
  assertEquals(MessageRules.message("1500,5", clock), MessageRules.message("1500.50", clock))
  assertNotNull(MessageRules.amount("999999999,99"))
  assertNotNull(MessageRules.amount("0,01"))
 }
 @Test fun includesContactNameForEveryGreeting() {
  assertEquals("Hola buen día María López, lo de hoy es: $1.500,00",
   MessageRules.message("1500", clock, "  María   López  "))
  assertEquals("Hola buenas tardes Juan, lo de hoy es: $1.500,00",
   MessageRules.message("1500", Clock.offset(clock, Duration.ofHours(4)), "Juan"))
  assertEquals("Hola buenas noches Juan, lo de hoy es: $1.500,00",
   MessageRules.message("1500", Clock.offset(clock, Duration.ofHours(12)), "Juan"))
  assertEquals("Hola buen día, lo de hoy es: $1.500,00",
   MessageRules.message("1500", clock, "   "))
 }
 @Test fun invalidAmounts() {
  listOf("", "0", "-1", "1.500", "1,2.3", "NaN", "1000000000", " 20", "1e3").forEach {
   assertNull(it, MessageRules.amount(it))
  }
 }
 @Test fun normalizesBuenosAiresMobileFormats() {
  listOf("23456789", "15 2345-6789", "11 2345-6789", "(011) 2345-6789",
   "11 15 2345-6789", "(011) 15 2345-6789", "+54 9 11 2345-6789",
   "+54 11 2345-6789", "5491123456789", "541123456789").forEach {
   assertEquals(it, "5491123456789", MessageRules.internationalPhone(it))
  }
 }
 @Test fun rejectsOtherAreasAndMalformedPhones() {
  listOf("", "123", "+16502530000", "+54 9 221 2345678", "0221 2345678",
   "+54 9 11 234567890", "11234567890", "11ABC23456789", "11 23456789 ext 9",
   "++5491123456789").forEach { assertNull(it, MessageRules.internationalPhone(it)) }
 }
 @Test fun urlRoundTripPreservesText() {
  val message = MessageRules.message("1500", clock)!!
  val url = MessageRules.whatsappUrl("23456789", message)
  assertTrue(url.startsWith("https://wa.me/5491123456789?text="))
  assertEquals(message, URLDecoder.decode(url.substringAfter("?text="), "UTF-8"))
  assertFalse(url.contains(" "))
 }
 @Test fun usesDeviceTimeZone() {
  assertTrue(MessageRules.message("1", clock.withZone(ZoneId.of("Asia/Tokyo")))!!.startsWith("Hola buenas noches"))
 }
}

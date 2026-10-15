package ar.com.fiado

import ar.com.fiado.readings.*
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class AutomaticMessageRulesTest {
 private val clock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"))
 private fun reading(vararg movements: Movement) = Reading("fixture", listOf(client(*movements).let {
  it.copy(serverTotal = ReviewRules.total(it))
 }), emptyList())
 @Test fun alwaysUsesComplicatedFormatEvenWithoutPreviousBalance() {
  val reading = reading(movement(MovementType.CARGO, "4200"), movement(MovementType.CARGO, "13000"))
  val draft = AutomaticMessageRules.draft(reading, 0, true, reviewContact, clock)!!
  assertEquals("+5491123456789", draft.phone)
  assertEquals("Hola buen día Leonardo, hasta el día de hoy sería:\n```\n" +
   "+      $  4.200\n+      $ 13.000\n---------------\nTOTAL  $ 17.200\n```", draft.message)
  assertFalse(draft.message.contains("Saldo"))
 }
 @Test fun matchesExistingComplicatedMessageForBalanceAndCharges() {
  val draft = AutomaticMessageRules.draft(reading(movement(MovementType.SALDO, "13800"),
   movement(MovementType.CARGO, "9200")), 0, true, reviewContact, clock)!!
  assertEquals(MessageRules.detailedMessage("13800", listOf("9200"), clock, "Leonardo"), draft.message)
 }
 @Test fun paymentsAreSubtractedAndEachMovementIsPreserved() {
  val draft = AutomaticMessageRules.draft(reading(movement(MovementType.SALDO, "10000"),
   movement(MovementType.CARGO, "4200"), movement(MovementType.CARGO, "13000"),
   movement(MovementType.PAGO, "3000")), 0, true, reviewContact, clock)!!
  assertTrue(draft.message.contains("- Pago $  3.000"))
  assertTrue(draft.message.contains("TOTAL  $ 24.200"))
 }
 @Test fun totalAboveSimpleLimitDoesNotTruncateDigits() {
  val draft = AutomaticMessageRules.draft(reading(movement(MovementType.CARGO, "999999999"),
   movement(MovementType.CARGO, "1")), 0, true, reviewContact, clock)!!
  assertTrue(draft.message.contains("1.000.000.000"))
  assertTrue(draft.message.contains("999.999.999"))
 }
 @Test fun contactNameIsUsedEvenWhenOcrNameIsMissing() {
  val input = reading(movement(MovementType.CARGO, "10")).let { it.copy(clients = listOf(it.clients.single().copy(name = ""))) }
  assertTrue(AutomaticMessageRules.draft(input, 0, true, reviewContact, clock)!!.message.startsWith("Hola buen día Leonardo,"))
  assertNull(AutomaticMessageRules.draft(input, 0, true, null, clock))
  assertNull(AutomaticMessageRules.draft(input, 0, true, reviewContact.copy(phone = "123"), clock))
 }
 @Test fun unknownAmountsAndTotalsBlockSharingInsteadOfInventingNumbers() {
  val input = reading(movement(MovementType.CARGO, "5000"))
  listOf(
   input.copy(clients = listOf(input.clients.single().copy(serverTotal = null))),
   input.copy(clients = listOf(input.clients.single().copy(serverTotal = BigDecimal("6000")))),
   reading(movement(MovementType.CARGO, "")),
   reading(movement(MovementType.CARGO, "1000000000")),
   reading(movement(MovementType.CARGO, "12,50"))
  ).forEach { assertNull(AutomaticMessageRules.draft(it, 0, true, reviewContact, clock)) }
 }
 @Test fun zeroNegativeAndMultiplePreviousBalancesBlockSharing() {
  listOf(
   reading(movement(MovementType.CARGO, "100"), movement(MovementType.PAGO, "100")),
   reading(movement(MovementType.PAGO, "200")),
   reading(movement(MovementType.SALDO, "100"), movement(MovementType.SALDO, "200")),
   reading()
  ).forEach { assertNull(AutomaticMessageRules.draft(it, 0, true, reviewContact, clock)) }
 }
 @Test fun multipleClientsRequireSelectionAndNeverCombineTheirBalances() {
  val first = reading(movement(MovementType.CARGO, "1000"))
  val second = reading(movement(MovementType.CARGO, "9000")).clients.single()
  val both = first.copy(clients = first.clients + second)
  assertNull(AutomaticMessageRules.draft(both, 0, false, reviewContact, clock))
  val draft = AutomaticMessageRules.draft(both, 1, true, reviewContact, clock)!!
  assertTrue(draft.message.contains("9.000"))
  assertFalse(draft.message.contains("1.000"))
  assertNull(AutomaticMessageRules.draft(both, 100, true, reviewContact, clock))
 }
}

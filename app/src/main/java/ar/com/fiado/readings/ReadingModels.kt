package ar.com.fiado.readings

import ar.com.fiado.MessageRules
import java.math.BigDecimal

enum class MovementType(val wire: String, val label: String) {
 CARGO("cargo", "Cargo"), PAGO("pago", "Pago"), SALDO("saldo_anterior", "Saldo anterior")
}
data class Movement(
 val type: MovementType, val reference: String?, val detail: String?,
 val amount: String, val needsReview: Boolean, val included: Boolean = true
)
data class Candidate(val id: String, val name: String)
data class ClientReading(
 val name: String, val movements: List<Movement>, val warnings: List<String>,
 val needsReview: Boolean, val match: String, val candidates: List<Candidate>,
 val serverTotal: BigDecimal?
)
data class Reading(val requestId: String, val clients: List<ClientReading>, val warnings: List<String>)
data class ReviewContact(val name: String, val originalPhone: String, val phone: String)
data class AppliedReading(val name: String, val originalPhone: String, val phone: String,
 val total: String, val previous: String? = null, val additions: List<String> = emptyList())

object ReviewRules {
 fun total(client: ClientReading): BigDecimal? {
  val included = client.movements.filter { it.included }
  if (included.isEmpty()) return null
  return included.fold(BigDecimal.ZERO) { sum, movement ->
   val value = MessageRules.amount(movement.amount) ?: return null
   if (movement.type == MovementType.PAGO) sum - value else sum + value
  }
 }
 fun multipleBalances(client: ClientReading) = client.movements.count { it.included && it.type == MovementType.SALDO } > 1
 fun canUseComplicated(client: ClientReading): Boolean {
  val included = client.movements.filter { it.included }
  return included.count { it.type == MovementType.SALDO } == 1 &&
   included.any { it.type == MovementType.CARGO } && included.none { it.type == MovementType.PAGO } &&
   included.all { MessageRules.amount(it.amount) != null }
 }
 fun application(client: ClientReading, contact: ReviewContact?, complicated: Boolean, confirmed: Boolean): AppliedReading? {
  if (!confirmed || contact == null || client.name.isBlank()) return null
  val normalized = MessageRules.internationalPhone(contact.phone) ?: return null
  val total = total(client)?.takeIf { it > BigDecimal.ZERO } ?: return null
  if (complicated && !canUseComplicated(client)) return null
  if (!complicated && MessageRules.amount(total.toPlainString()) == null) return null
  val included = client.movements.filter { it.included }
  return AppliedReading(client.name.trim(), contact.originalPhone, "+$normalized", total.toPlainString(),
   if (complicated) included.single { it.type == MovementType.SALDO }.amount else null,
   if (complicated) included.filter { it.type == MovementType.CARGO }.map { it.amount } else emptyList())
 }
}

package ar.com.fiado.readings

import ar.com.fiado.MessageRules
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Clock
import java.time.LocalTime
import java.util.Locale

data class WhatsAppDraft(val phone: String, val message: String)

/** Photo-only flow: the final WhatsApp action confirms the visible photo, account and contact. */
object AutomaticMessageRules {
 fun problem(reading: Reading, index: Int, selectedExplicitly: Boolean, manuallyCorrected: Boolean = false): String? {
  if (!selectedExplicitly) return "La foto tiene varios clientes. Elegí la cuenta que querés cobrar."
  val client = reading.clients.getOrNull(index) ?: return "No se encontró una cuenta en la foto. Podés completarla a mano."
  localProblem(client)?.let { return it }
  if (!manuallyCorrected) {
   if (client.serverTotal == null) return "El backend no pudo confirmar el total. Revisá y corregí la lectura."
   if (ReviewRules.total(client)!!.compareTo(client.serverTotal) != 0)
    return "Los movimientos no coinciden con el total del backend. Revisá y corregí la lectura."
  }
  return null
 }

 fun localProblem(client: ClientReading): String? {
  val movements = client.movements.filter { it.included }
  if (movements.isEmpty()) return "Agregá al menos un movimiento para esta cuenta."
  if (movements.any { MessageRules.amount(it.amount) == null })
   return "Hay importes incompletos o inválidos. Corregilos o excluí esos movimientos."
  if (ReviewRules.multipleBalances(client))
   return "Hay más de un saldo anterior. Corregí el tipo o excluí los saldos duplicados."
  val total = ReviewRules.total(client) ?: return "Completá los importes para calcular el total."
  if (total <= BigDecimal.ZERO) return "Esta cuenta no tiene un total positivo para cobrar."
  return null
 }

 fun draft(reading: Reading, index: Int, selectedExplicitly: Boolean, contact: ReviewContact?,
  clock: Clock = Clock.systemDefaultZone(), manuallyCorrected: Boolean = false): WhatsAppDraft? {
  if (problem(reading, index, selectedExplicitly, manuallyCorrected) != null || contact == null) return null
  val phone = MessageRules.internationalPhone(contact.phone) ?: return null
  val client = reading.clients[index]
  val movements = client.movements.filter { it.included }
  val total = requireNotNull(ReviewRules.total(client))
  val formatter = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.forLanguageTag("es-AR")))
  val width = (movements.map { formatter.format(requireNotNull(MessageRules.amount(it.amount))).length } +
   formatter.format(total).length).max()
  fun row(label: String, value: BigDecimal) = "${label.padEnd(7)}$ ${formatter.format(value).padStart(width)}"
  val name = contact.name.trim().ifBlank { client.name.trim() }.replace(Regex("\\s+"), " ")
  val recipient = if (name.isEmpty()) "" else " $name"
  val message = buildString {
   append("${MessageRules.greeting(LocalTime.now(clock))}$recipient, hasta el día de hoy sería:\n```\n")
   val ordered = movements.filter { it.type == MovementType.SALDO } + movements.filter { it.type != MovementType.SALDO }
   ordered.forEach { movement ->
    val label = when (movement.type) {
     MovementType.SALDO -> "Saldo"
     MovementType.CARGO -> "+"
     MovementType.PAGO -> "- Pago"
    }
    append(row(label, requireNotNull(MessageRules.amount(movement.amount)))).append('\n')
   }
   append("-".repeat(9 + width)).append('\n')
   append(row("TOTAL", total)).append("\n```")
  }
  return WhatsAppDraft("+$phone", message)
 }
}

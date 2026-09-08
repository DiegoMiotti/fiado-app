package ar.com.fiado

import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.math.BigDecimal
import java.net.URLEncoder
import java.time.Clock
import java.time.LocalTime
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MessageRules {
 fun greeting(time: LocalTime): String = when (time.hour) {
  in 6..11 -> "Hola buen día"
  in 12..19 -> "Hola buenas tardes"
  else -> "Hola buenas noches"
 }
 fun amount(raw: String): BigDecimal? {
  if (raw.isEmpty() || !Regex("[0-9.]+").matches(raw)) return null
  if (raw.startsWith(".") || raw.endsWith(".") || ".." in raw) return null
  val parts = raw.split(".")
  val digits = if (parts.size == 1) {
   if (!Regex("[0-9]{1,9}").matches(raw)) return null
   raw
  } else {
   if (!Regex("[0-9]{1,3}").matches(parts.first())) return null
   if (parts.drop(1).any { !Regex("[0-9]{3}").matches(it) }) return null
   val joined = parts.joinToString("")
   if (joined.length !in 1..9 || !Regex("[0-9]{1,9}").matches(joined)) return null
   // Rechazar ceros a la izquierda en formato con miles (ej: "01.500").
   if (joined.length > 1 && joined.startsWith("0")) return null
   joined
  }
  return digits.toBigDecimalOrNull()?.takeIf {
   it > BigDecimal.ZERO && it <= BigDecimal("999999999")
  }
 }
 /** Formatea dígitos sueltos a vista con punto de miles, ej: "1500" -> "1.500". */
 fun formatInput(raw: String): String {
  val digits = raw.filter(Char::isDigit).take(9).trimStart('0')
  if (digits.isEmpty()) return ""
  return DecimalFormat("#,##0", DecimalFormatSymbols(Locale.forLanguageTag("es-AR")))
   .format(digits.toBigDecimalOrNull() ?: return "")
 }
 fun message(raw: String, clock: Clock = Clock.systemDefaultZone(), contactName: String = ""): String? {
  val value = amount(raw) ?: return null
  val formatted = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.forLanguageTag("es-AR"))).format(value)
  val name = contactName.trim().replace(Regex("\\s+"), " ")
  val recipient = if (name.isEmpty()) "" else " $name"
  return "${greeting(LocalTime.now(clock))}$recipient, lo de hoy es: $$formatted"
 }
 fun detailedMessage(previous: String, additions: List<String>, contactName: String = ""): String? {
  val balance = amount(previous) ?: return null
  if (additions.isEmpty()) return null
  val values = additions.map { amount(it) ?: return null }
  val formatter = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.forLanguageTag("es-AR")))
  val name = contactName.trim().replace(Regex("\\s+"), " ")
  return buildString {
   if (name.isNotEmpty()) append("$name, entonces sería:\n") else append("Entonces sería:\n")
   append("${formatter.format(balance)} anterior\n")
   values.forEach { append("+ ${formatter.format(it)}\n") }
   append("----------\n")
   append(formatter.format(values.fold(balance, BigDecimal::add)))
  }
 }
 // This pilot uses Buenos Aires mobile numbers with the user-specified +54 9 11 prefix.
 // Match complete known formats; never truncate a different area code or country.
 fun internationalPhone(raw: String): String? {
  val input = raw.trim()
  if (!Regex("\\+?[0-9 ()-]+").matches(input)) return null
  val digits = input.filter(Char::isDigit)
  val subscriber = when {
   input.startsWith("+") && digits.matches(Regex("54911[0-9]{8}")) -> digits.takeLast(8)
   input.startsWith("+") && digits.matches(Regex("5411[0-9]{8}")) -> digits.takeLast(8)
   input.startsWith("+") -> return null
   digits.matches(Regex("[0-9]{8}")) -> digits
   digits.matches(Regex("15[0-9]{8}")) -> digits.drop(2)
   digits.matches(Regex("11[0-9]{8}")) -> digits.drop(2)
   digits.matches(Regex("011[0-9]{8}")) -> digits.drop(3)
   digits.matches(Regex("1115[0-9]{8}")) -> digits.drop(4)
   digits.matches(Regex("01115[0-9]{8}")) -> digits.drop(5)
   digits.matches(Regex("54911[0-9]{8}")) -> digits.takeLast(8)
   digits.matches(Regex("5411[0-9]{8}")) -> digits.takeLast(8)
   else -> return null
  }
  return try {
   val util = PhoneNumberUtil.getInstance()
   val parsed = util.parse("+54911$subscriber", "ZZ")
   if (!util.isValidNumber(parsed)) null
   else util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164).removePrefix("+")
  } catch (_: com.google.i18n.phonenumbers.NumberParseException) { null }
 }
 fun whatsappUrl(phone: String, message: String): String {
  val number = requireNotNull(internationalPhone(phone))
  val encoded = URLEncoder.encode(message, "UTF-8").replace("+", "%20")
  return "https://wa.me/$number?text=$encoded"
 }
}

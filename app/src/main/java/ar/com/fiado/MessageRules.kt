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
  if (!Regex("[0-9]{1,9}([.,][0-9]{1,2})?").matches(raw)) return null
  return raw.replace(',', '.').toBigDecimalOrNull()?.takeIf {
   it > BigDecimal.ZERO && it <= BigDecimal("999999999.99")
  }
 }
 fun message(raw: String, clock: Clock = Clock.systemDefaultZone(), contactName: String = ""): String? {
  val value = amount(raw) ?: return null
  val formatted = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.forLanguageTag("es-AR"))).format(value)
  val name = contactName.trim().replace(Regex("\\s+"), " ")
  val recipient = if (name.isEmpty()) "" else " $name"
  return "${greeting(LocalTime.now(clock))}$recipient, lo de hoy es: $$formatted"
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

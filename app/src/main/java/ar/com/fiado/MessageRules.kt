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
  in 6..11 -> "Buenos días"
  in 12..19 -> "Buenas tardes"
  else -> "Buenas noches"
 }
 fun amount(raw: String): BigDecimal? {
  if (!Regex("[0-9]{1,9}([.,][0-9]{1,2})?").matches(raw)) return null
  return raw.replace(',', '.').toBigDecimalOrNull()?.takeIf {
   it > BigDecimal.ZERO && it <= BigDecimal("999999999.99")
  }
 }
 fun message(raw: String, clock: Clock = Clock.systemDefaultZone()): String? {
  val value = amount(raw) ?: return null
  val formatted = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.forLanguageTag("es-AR"))).format(value)
  return "${greeting(LocalTime.now(clock))}, el fiado de hoy es $$formatted."
 }
 // An explicit country code avoids guessing from a local contact number.
 fun internationalPhone(raw: String): String? {
  if (!Regex("\\+[0-9 ()-]+").matches(raw)) return null
  return try {
   val util = PhoneNumberUtil.getInstance()
   val parsed = util.parse(raw, "ZZ")
   if (!util.isValidNumber(parsed) || parsed.hasExtension()) null
   else util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164).removePrefix("+")
  } catch (_: com.google.i18n.phonenumbers.NumberParseException) { null }
 }
 fun whatsappUrl(phone: String, message: String): String {
  val number = requireNotNull(internationalPhone(phone))
  val encoded = URLEncoder.encode(message, "UTF-8").replace("+", "%20")
  return "https://wa.me/$number?text=$encoded"
 }
}

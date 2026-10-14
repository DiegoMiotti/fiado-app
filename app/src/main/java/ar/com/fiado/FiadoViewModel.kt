package ar.com.fiado
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class FiadoViewModel(private val saved: SavedStateHandle) : ViewModel() {
 val amount = saved.getStateFlow("amount", "")
 val name = saved.getStateFlow("name", "")
 val originalPhone = saved.getStateFlow("originalPhone", "")
 val phone = saved.getStateFlow("phone", "")
 val complicated = saved.getStateFlow("complicated", false)
 val previous = saved.getStateFlow("previous", "")
 val additions = saved.getStateFlow("additions", arrayListOf(""))
 fun complicated(value: Boolean) { saved["complicated"] = value }
 fun previous(value: String) { saved["previous"] = value }
 fun addition(index: Int, value: String) {
  saved["additions"] = ArrayList(additions.value).apply { set(index, value) }
 }
 fun addAmount() { saved["additions"] = ArrayList(additions.value).apply { add("") } }
 fun removeAmount(index: Int) {
  saved["additions"] = ArrayList(additions.value).apply { removeAt(index) }
 }
 fun amount(value: String) { saved["amount"] = value }
 fun phone(value: String) { saved["phone"] = value }
 fun contact(name: String, phone: String) {
  saved["name"] = name
  saved["originalPhone"] = phone
  saved["phone"] = MessageRules.internationalPhone(phone)?.let { "+$it" } ?: phone
 }
 fun applyReading(reading: ar.com.fiado.readings.AppliedReading) {
  // Validate the entire replacement before touching the existing draft.
  require(reading.name.isNotBlank() && MessageRules.internationalPhone(reading.phone) != null)
  if (reading.previous == null) require(MessageRules.amount(reading.total) != null)
  else {
   require(MessageRules.amount(reading.previous) != null && reading.additions.isNotEmpty())
   val values = reading.additions.map { requireNotNull(MessageRules.amount(it)) }
   require(values.fold(requireNotNull(MessageRules.amount(reading.previous)), java.math.BigDecimal::add)
    .compareTo(reading.total.toBigDecimal()) == 0)
  }
  saved["amount"] = if (reading.previous == null) MessageRules.formatInput(reading.total) else ""
  saved["complicated"] = reading.previous != null
  saved["previous"] = reading.previous?.let(MessageRules::formatInput).orEmpty()
  saved["additions"] = if (reading.previous == null) arrayListOf("")
   else ArrayList(reading.additions.map(MessageRules::formatInput))
  saved["name"] = reading.name
  saved["originalPhone"] = reading.originalPhone
  saved["phone"] = reading.phone
 }
 fun clear() {
  listOf("amount", "name", "originalPhone", "phone").forEach { saved[it] = "" }
  saved["complicated"] = false
  saved["previous"] = ""
  saved["additions"] = arrayListOf("")
 }
}

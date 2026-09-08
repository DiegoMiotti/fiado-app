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
 fun clear() {
  listOf("amount", "name", "originalPhone", "phone").forEach { saved[it] = "" }
  saved["complicated"] = false
  saved["previous"] = ""
  saved["additions"] = arrayListOf("")
 }
}

package ar.com.fiado
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class FiadoViewModel(private val saved: SavedStateHandle) : ViewModel() {
 val amount = saved.getStateFlow("amount", "")
 val name = saved.getStateFlow("name", "")
 val originalPhone = saved.getStateFlow("originalPhone", "")
 val phone = saved.getStateFlow("phone", "")
 fun amount(value: String) { saved["amount"] = value }
 fun phone(value: String) { saved["phone"] = value }
 fun contact(name: String, phone: String) {
  saved["name"] = name
  saved["originalPhone"] = phone
  saved["phone"] = phone
 }
 fun clear() {
  listOf("amount", "name", "originalPhone", "phone").forEach { saved[it] = "" }
 }
}

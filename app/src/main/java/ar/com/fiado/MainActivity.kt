package ar.com.fiado

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import ar.com.fiado.readings.*

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  enableEdgeToEdge(
   statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
   navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
  )
  setContent {
   FiadoTheme { PhotoScreen() }
  }
 }

 @Composable private fun PhotoScreen() {
  val factory = remember {
   object : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
     ReadingViewModel(KeystoreConnectionStore(applicationContext), HttpReadingGateway(), AndroidPhotoPreparer(applicationContext)) as T
   }
  }
  val model: ReadingViewModel = viewModel(factory = factory)
  ReadingPanel(model, ::openWhatsApp, ::readPhone)
 }

 private fun readPhone(uri: Uri): Pair<String, String>? = try {
  contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
   ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)?.use { cursor ->
   if (!cursor.moveToFirst()) null else {
    val number = cursor.getString(1).orEmpty()
    if (number.isBlank()) null else cursor.getString(0).orEmpty() to number
   }
  }
 } catch (_: RuntimeException) { null }

 private fun openWhatsApp(phone: String, message: String): Boolean {
  if (MessageRules.internationalPhone(phone) == null) return false
  val uri = MessageRules.whatsappUrl(phone, message).toUri()
  val options = listOf("com.whatsapp", "com.whatsapp.w4b").map { pkg ->
   Intent(Intent.ACTION_VIEW, uri).setPackage(pkg)
  }.filter { it.resolveActivity(packageManager) != null }
  val intent = when (options.size) {
   0 -> Intent(Intent.ACTION_VIEW, uri)
   1 -> options.first()
   else -> Intent.createChooser(options.first(), getString(R.string.whatsapp_picker)).apply {
    putExtra(Intent.EXTRA_INITIAL_INTENTS, options.drop(1).toTypedArray())
   }
  }
  return try { startActivity(intent); true }
  catch (_: android.content.ActivityNotFoundException) { false }
  catch (_: SecurityException) { false }
 }
}

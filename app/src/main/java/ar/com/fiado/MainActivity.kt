package ar.com.fiado

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Clock

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  enableEdgeToEdge(
   statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
   navigationBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
  )
  setContent {
   MaterialTheme(colorScheme = lightColorScheme(
    primary = Color(0xFF135D49), onPrimary = Color.White,
    background = Color(0xFFF6F8F3), surface = Color(0xFFF6F8F3),
    secondaryContainer = Color(0xFFE1EEE5), onSecondaryContainer = Color(0xFF163B2F)
   )) { FiadoScreen() }
  }
 }

 @Composable
 private fun FiadoScreen(model: FiadoViewModel = viewModel()) {
  val amount by model.amount.collectAsStateWithLifecycle()
  val name by model.name.collectAsStateWithLifecycle()
  val originalPhone by model.originalPhone.collectAsStateWithLifecycle()
  val phone by model.phone.collectAsStateWithLifecycle()
  val complicated by model.complicated.collectAsStateWithLifecycle()
  val previous by model.previous.collectAsStateWithLifecycle()
  val additions by model.additions.collectAsStateWithLifecycle()
  var clock by remember { mutableStateOf(Clock.systemDefaultZone()) }
  fun currentMessage(): String? = if (complicated) MessageRules.detailedMessage(previous, additions, clock)
   else MessageRules.message(amount, clock, name)
  val message = currentMessage()
  val scope = rememberCoroutineScope()
  val snackbar = remember { SnackbarHostState() }
  val lifecycle = LocalLifecycleOwner.current.lifecycle
  fun notice(resource: Int) { scope.launch { snackbar.showSnackbar(getString(resource)) } }
  LaunchedEffect(lifecycle) {
   lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
    while (true) {
     clock = Clock.fixed(java.time.Instant.now(), java.time.ZoneId.systemDefault())
     delay(1000)
    }
   }
  }
  val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
   if (result.resultCode == RESULT_OK) {
    val uri = result.data?.data
    scope.launch {
     val selected = withContext(Dispatchers.IO) { uri?.let { readPhone(it) } }
     if (selected == null) notice(R.string.contact_error)
     else model.contact(selected.first, selected.second)
    }
   }
  }
  Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
   Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
    .padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(stringResource(R.string.today), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
    Text(stringResource(R.string.subtitle), style = MaterialTheme.typography.bodyLarge)
    HorizontalDivider()
    Text(name.ifEmpty { stringResource(R.string.no_contact) }, style = MaterialTheme.typography.titleMedium)
    OutlinedButton(onClick = {
     try {
      picker.launch(Intent(Intent.ACTION_PICK).apply { type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE })
     } catch (_: android.content.ActivityNotFoundException) { notice(R.string.picker_error) }
       catch (_: SecurityException) { notice(R.string.picker_error) }
    }, modifier = Modifier.fillMaxWidth()) {
     Text(stringResource(if (originalPhone.isEmpty()) R.string.choose_contact else R.string.change_contact))
    }
    if (originalPhone.isNotEmpty()) {
     Text(stringResource(R.string.original_phone, originalPhone), style = MaterialTheme.typography.bodySmall)
     OutlinedTextField(value = phone, onValueChange = model::phone, modifier = Modifier.fillMaxWidth(),
      label = { Text(stringResource(R.string.phone)) }, singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
      isError = MessageRules.internationalPhone(phone) == null,
      supportingText = { Text(stringResource(if (MessageRules.internationalPhone(phone) == null) R.string.phone_error else R.string.phone_help)) })
    }
    if (originalPhone.isNotEmpty()) {
     MessageRules.internationalPhone(phone)?.let { normalized ->
      Text(stringResource(R.string.normalized_phone, normalized), style = MaterialTheme.typography.bodyMedium)
     }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
     verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
     Text(stringResource(R.string.complicated), style = MaterialTheme.typography.titleMedium)
     Switch(checked = complicated, onCheckedChange = model::complicated)
    }
    if (complicated) {
     Text(stringResource(R.string.complicated_help))
     OutlinedTextField(value = previous, onValueChange = { model.previous(MessageRules.formatInput(it)) },
      modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.previous_balance)) },
      prefix = { Text("$") }, singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
     additions.forEachIndexed { index, value ->
      OutlinedTextField(value = value, onValueChange = { model.addition(index, MessageRules.formatInput(it)) },
       modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.addition_amount, index + 1)) },
       prefix = { Text("+ $") }, singleLine = true,
       keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
       trailingIcon = if (additions.size > 1) { {
        TextButton(onClick = { model.removeAmount(index) }) { Text(stringResource(R.string.remove_amount)) }
       } } else null)
     }
     OutlinedButton(onClick = model::addAmount, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.add_amount))
     }
    } else {
     OutlinedTextField(value = amount, onValueChange = { model.amount(MessageRules.formatInput(it)) }, modifier = Modifier.fillMaxWidth(),
      label = { Text(stringResource(R.string.amount)) }, prefix = { Text("$") }, singleLine = true,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
     isError = amount.isNotEmpty() && message == null,
     supportingText = { Text(stringResource(if (amount.isNotEmpty() && message == null) R.string.amount_error else R.string.amount_help)) })
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
     Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text(stringResource(R.string.preview), style = MaterialTheme.typography.labelMedium)
      SelectionContainer { Text(message ?: stringResource(if (complicated) R.string.detailed_preview_empty else R.string.preview_empty), style = MaterialTheme.typography.titleLarge) }
     }
    }
    OutlinedButton(onClick = {
     clock = Clock.fixed(java.time.Instant.now(), java.time.ZoneId.systemDefault())
     currentMessage()?.let { text ->
      try { getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), text)) }
      catch (_: RuntimeException) { notice(R.string.copy_error) }
     }
    }, enabled = message != null, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.copy)) }
    Button(onClick = {
     clock = Clock.fixed(java.time.Instant.now(), java.time.ZoneId.systemDefault())
     currentMessage()?.let { text ->
      if (!openWhatsApp(phone, text)) notice(R.string.whatsapp_error)
     }
    }, enabled = message != null && originalPhone.isNotEmpty() && MessageRules.internationalPhone(phone) != null,
     modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_whatsapp)) }
    Text(stringResource(R.string.send_help), style = MaterialTheme.typography.bodySmall)
    TextButton(onClick = model::clear, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.clear)) }
   }
  }
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

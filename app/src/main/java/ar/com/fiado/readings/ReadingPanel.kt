package ar.com.fiado.readings

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.com.fiado.MessageRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun contactPickerIntent(): Intent =
 Intent(Intent.ACTION_PICK).setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE)

@Composable
fun ReadingPanel(model: ReadingViewModel, openWhatsApp: (String, String) -> Boolean,
 readContact: (Uri) -> Pair<String, String>?) {
 val state by model.state.collectAsStateWithLifecycle()
 var settings by remember { mutableStateOf(false) }
 var notice by remember { mutableStateOf<String?>(null) }
 var contactSession by rememberSaveable { mutableIntStateOf(0) }
 // A cancelled UI-scope lookup after rotation must not leave a restored "pending" flag stuck forever.
 var contactPending by remember { mutableStateOf(false) }
 var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
 val scope = rememberCoroutineScope()
 val context = LocalContext.current
 val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
  val uri = cameraUri?.toUri()
  cameraUri = null
  if (saved && uri != null) { notice = null; model.choosePhoto(uri.toString(), autoRead = true) }
  else uri?.let { CameraPhotos.discard(context, it) }
 }
 val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
  uri?.let { notice = null; model.choosePhoto(it.toString(), autoRead = true) }
 }
 val contacts = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
  if (result.resultCode != android.app.Activity.RESULT_OK) contactPending = false
  else {
   val session = contactSession
   scope.launch {
    try {
     val contact = withContext(Dispatchers.IO) { result.data?.data?.let(readContact) }
     if (contact == null) notice = "No se pudo leer el contacto. Elegí otro contacto."
     else { model.contact(session, contact.first, contact.second); notice = null }
    } finally { contactPending = false }
   }
  }
 }
 BackHandler(enabled = state.photo != null || state.busy) { model.cancel(); notice = null }
 Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
  Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState())
   .padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
   Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
     Text("Fiado", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
     Text("Foto → contacto → WhatsApp", style = MaterialTheme.typography.titleMedium)
     Text("La cuenta se arma automáticamente, siempre con el detalle de Complicados.")
    }
   }
   Text("Al confirmar la foto se enviará a tu backend y al proveedor de IA. Cada lectura puede consumir cuota y saldo.",
    style = MaterialTheme.typography.bodySmall)
   val captureEnabled = state.hasToken && !state.configBusy && !state.busy && cameraUri == null && !contactPending
   Button(onClick = {
    try {
     val uri = CameraPhotos.create(context)
     cameraUri = uri.toString()
     camera.launch(uri)
    } catch (_: Exception) {
     cameraUri?.let { CameraPhotos.discard(context, it.toUri()) }
     cameraUri = null
     notice = "No se pudo abrir la cámara. Podés elegir una foto guardada."
    }
   }, enabled = captureEnabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
    Text(if (state.photo == null) "Sacar foto" else "Sacar otra foto")
   }
   OutlinedButton(onClick = {
    try { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    catch (_: RuntimeException) { notice = "No se pudo abrir el selector de fotos." }
   }, enabled = captureEnabled, modifier = Modifier.fillMaxWidth()) { Text("Elegir foto guardada") }
   if (!state.hasToken && !state.configBusy) WarningText("Configurá la conexión antes de sacar la foto.")
   TextButton(onClick = { settings = true }, enabled = !state.busy && !contactPending) { Text("Conexión del backend") }
   state.photo?.preview?.let {
    Image(it.asImageBitmap(), "Foto de la cuenta", modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp))
   }
   if (state.busy) {
    CircularProgressIndicator()
    Text(if (state.photo == null) "Preparando foto…" else "Leyendo la cuenta y armando el mensaje…")
   }
   state.notice?.let { WarningText(it) }
   notice?.let { WarningText(it) }
   if (state.photo != null && state.reading == null && !state.busy && state.notice != null) {
    OutlinedButton(onClick = model::readPhoto, enabled = state.hasToken && !state.configBusy,
     modifier = Modifier.fillMaxWidth()) { Text("Reintentar lectura") }
    FilledTonalButton(onClick = model::beginCorrection, modifier = Modifier.fillMaxWidth()) { Text("Completar cuenta a mano") }
   }
   state.reading?.let { reading ->
    if (reading.clients.size > 1) {
     Text("La foto tiene varias cuentas. Elegí una:", style = MaterialTheme.typography.titleMedium)
     reading.clients.forEachIndexed { index, client ->
      OutlinedButton(onClick = { model.select(index); notice = null }, enabled = !contactPending,
       modifier = Modifier.fillMaxWidth()) {
       Text("${if (state.selectedExplicitly && state.selected == index) "✓ " else ""}${index + 1}. ${client.name.ifBlank { "Sin nombre" }}")
      }
     }
    }
    if (state.selectedExplicitly) state.client?.let { client ->
     Text("Cuenta en el papel: ${client.name.ifBlank { "sin nombre" }}", style = MaterialTheme.typography.titleMedium)
     ReadingSummary(client, reading.warnings)
     if (state.selected in state.correctedClients) Text("Cuenta corregida a mano. El total se recalculó con tus movimientos.")
     OutlinedButton(onClick = model::beginCorrection, enabled = !contactPending,
      modifier = Modifier.fillMaxWidth()) { Text("Corregir lectura") }
    }
    val problem = state.automaticProblem
    if (problem != null) WarningText(problem)
    else {
     FilledTonalButton(onClick = {
      contactSession = model.session
      contactPending = true
      try { contacts.launch(contactPickerIntent()) }
      catch (_: RuntimeException) { contactPending = false; notice = "No se pudo abrir el selector de contactos." }
     }, enabled = !contactPending, modifier = Modifier.fillMaxWidth()) {
      Text(if (state.contact == null) "Elegir contacto" else "Cambiar contacto")
     }
     if (contactPending) Text("Esperando contacto…")
    }
   }
   state.contact?.let { contact ->
    Text("Contacto: ${contact.name.ifBlank { "sin nombre" }}")
    val phone = MessageRules.internationalPhone(contact.phone)
    if (phone == null) WarningText("El teléfono no es válido para WhatsApp. Elegí otro contacto con celular del área 11.")
    else Text("WhatsApp: +$phone")
   }
   val draft = state.automaticDraft()
   if (draft != null) MessagePreview(draft.message)
   Button(onClick = {
    // Rebuild at the tap so the greeting follows the actual time, including after returning from WhatsApp.
    model.state.value.automaticDraft()?.let { current ->
     val opened = try { openWhatsApp(current.phone, current.message) } catch (_: RuntimeException) { false }
     notice = if (opened) null else "No se pudo abrir WhatsApp. La cuenta y el contacto siguen disponibles para reintentar."
    }
   }, enabled = draft != null && !state.busy && !contactPending,
    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Confirmar y abrir WhatsApp") }
   Text("Comprobá la foto, el detalle y el contacto. Al continuar confirmás esta cuenta. El envío lo confirmás en WhatsApp.",
    style = MaterialTheme.typography.bodySmall)
   if (state.photo != null || state.busy) TextButton(onClick = { model.cancel(); notice = null },
    modifier = Modifier.fillMaxWidth()) { Text("Cancelar cuenta") }
  }
 }
 if (settings) ConnectionDialog(state, model) { settings = false }
 state.correction?.let { correction -> CorrectionDialog(state, correction, model) }
}

@Composable private fun CorrectionDialog(state: ReadingState, correction: ReadingCorrection, model: ReadingViewModel) {
 Dialog(onDismissRequest = model::cancelCorrection, properties = DialogProperties(usePlatformDefaultWidth = false)) {
  BackHandler { model.cancelCorrection() }
  Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
   Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Corregir cuenta", style = MaterialTheme.typography.headlineSmall)
    Text("Compará los movimientos con la foto. Podés completar importes, cambiar tipos, agregar filas o excluir totales duplicados. No se vuelve a consultar a la IA.")
    TextButton(onClick = model::cancelCorrection) { Text("Descartar correcciones") }
    state.photo?.preview?.let { Image(it.asImageBitmap(), "Foto para corregir la cuenta",
     modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) }
    OutlinedTextField(correction.client.name, model::correctionName, label = { Text("Nombre en el papel (opcional)") },
     modifier = Modifier.fillMaxWidth(), singleLine = true)
    correction.client.movements.forEachIndexed { index, movement ->
     Card(Modifier.fillMaxWidth()) {
      Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
       Row {
        Checkbox(movement.included, { value -> model.correctionMovement(index) { it.copy(included = value) } })
        Text("Usar movimiento ${index + 1}", modifier = Modifier.padding(top = 12.dp))
       }
       Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MovementType.entries.forEach { type ->
         FilterChip(selected = movement.type == type, enabled = movement.included,
          onClick = { model.correctionMovement(index) { it.copy(type = type) } }, label = { Text(type.label) })
        }
       }
       OutlinedTextField(movement.amount, { value -> model.correctionMovement(index) { it.copy(amount = value) } },
        label = { Text("Importe ${index + 1}") }, prefix = { Text("$") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = movement.included,
        isError = movement.included && MessageRules.amount(movement.amount) == null,
        supportingText = { Text("Pesos enteros de 1 a 999.999.999; no se recortan cifras.") })
       OutlinedTextField(movement.reference.orEmpty(),
        { value -> model.correctionMovement(index) { it.copy(reference = value.ifBlank { null }) } },
        label = { Text("Referencia ${index + 1} (opcional)") }, modifier = Modifier.fillMaxWidth(), enabled = movement.included)
       OutlinedTextField(movement.detail.orEmpty(),
        { value -> model.correctionMovement(index) { it.copy(detail = value.ifBlank { null }) } },
        label = { Text("Detalle ${index + 1} (opcional)") }, modifier = Modifier.fillMaxWidth(), enabled = movement.included)
      }
     }
    }
    OutlinedButton(onClick = model::addCorrectionMovement, modifier = Modifier.fillMaxWidth()) { Text("Agregar movimiento") }
    Text("Total recalculado: ${ReviewRules.total(correction.client)?.toPlainString()?.let { "$$it" } ?: "pendiente de completar"}",
     style = MaterialTheme.typography.titleLarge)
    val problem = AutomaticMessageRules.localProblem(correction.client)
    problem?.let { WarningText(it) }
    Text("Al guardar confirmás que los movimientos coinciden con el papel. Se usará este total local aunque la IA no haya podido calcularlo.")
    Button(onClick = { model.saveCorrection() }, enabled = problem == null,
     modifier = Modifier.fillMaxWidth()) { Text("Guardar correcciones") }
   }
  }
 }
}

@Composable private fun ReadingSummary(client: ClientReading, globalWarnings: List<String>) {
 Card(modifier = Modifier.fillMaxWidth()) {
  Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
   client.movements.filter { it.included }.forEach { movement ->
    Text("${movement.type.label}: $${movement.amount.ifBlank { "importe sin leer" }}")
    movement.reference?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    movement.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    if (movement.needsReview) WarningText("Compará este movimiento con la foto antes de confirmar.")
   }
   if (client.needsReview) Text("Revisá esta cuenta con la foto antes de abrir WhatsApp.")
   (globalWarnings + client.warnings).distinct().forEach { WarningText(it) }
  }
 }
}

@Composable private fun MessagePreview(message: String) {
 ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
  modifier = Modifier.fillMaxWidth()) {
  Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
   Text("Mensaje listo", style = MaterialTheme.typography.labelLarge)
   SelectionContainer {
    Column {
     Text(message.substringBefore("```"), style = MaterialTheme.typography.bodyLarge)
     Text(message.substringAfter("```\n").removeSuffix("\n```"), fontFamily = FontFamily.Monospace,
      modifier = Modifier.horizontalScroll(rememberScrollState()))
    }
   }
  }
 }
}

@Composable private fun WarningText(text: String) { Text(text, color = MaterialTheme.colorScheme.error) }

@Composable private fun ConnectionDialog(state: ReadingState, model: ReadingViewModel, onClose: () -> Unit) {
 var url by remember(state.url) { mutableStateOf(state.url) }
 // Credentials deliberately never enter saved instance state.
 var token by remember { mutableStateOf("") }
 Dialog(onDismissRequest = onClose, properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn)) {
  Surface(shape = MaterialTheme.shapes.large) {
   Column(Modifier.imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Conexión del backend", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(url, { url = it }, label = { Text("URL HTTPS") }, enabled = !state.configBusy,
     modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), singleLine = true)
    OutlinedTextField(token, { token = it }, label = { Text("Token privado del teléfono") }, enabled = !state.configBusy,
     modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true,
     keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
    Text(if (state.hasToken) "Hay un token guardado. Dejá el campo vacío para conservarlo en el mismo servidor."
     else "Ingresá el token del teléfono. Las claves de IA y Redis permanecen en Vercel.")
    state.settingsNotice?.let { Text(it) }
    Button(onClick = { model.saveConnection(url, token); token = "" }, enabled = !state.configBusy) { Text("Guardar conexión") }
    TextButton(onClick = { model.deleteConnection(); token = "" }, enabled = !state.configBusy) { Text("Borrar credencial") }
    TextButton(onClick = onClose) { Text("Cerrar") }
   }
  }
 }
}

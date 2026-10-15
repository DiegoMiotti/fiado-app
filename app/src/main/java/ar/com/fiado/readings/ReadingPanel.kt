package ar.com.fiado.readings

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
  Box(Modifier.fillMaxSize().padding(padding)) {
  Column(Modifier.widthIn(max = 600.dp).fillMaxSize().align(Alignment.TopCenter).imePadding().verticalScroll(rememberScrollState())
   .padding(horizontal = 20.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
     Text("Fiado", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.primary, modifier = Modifier.semantics { heading() })
     Text("Foto → contacto → WhatsApp", style = MaterialTheme.typography.titleMedium)
     Text("Prepará tu cobro con claridad.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
   SectionTitle("01 · Cargá la cuenta", "Empezá por la foto")
   Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
   Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
   Text("Los importes se leen de tu papel. Después podés revisarlos y corregirlos.")
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
   }, enabled = captureEnabled, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp), contentPadding = PaddingValues(16.dp)) {
    Text(if (state.photo == null) "Sacar foto" else "Sacar otra foto")
   }
   OutlinedButton(onClick = {
    try { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    catch (_: RuntimeException) { notice = "No se pudo abrir el selector de fotos." }
   }, enabled = captureEnabled, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Elegir foto guardada") }
   Text("Al confirmar la foto se enviará a tu backend y al proveedor de IA. Cada lectura puede consumir cuota y saldo.",
    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
   if (!state.hasToken && !state.configBusy) WarningText("Configurá la conexión antes de sacar la foto.")
   TextButton(onClick = { settings = true }, enabled = !state.busy && !contactPending,
    modifier = Modifier.heightIn(min = 48.dp)) { Text("Conexión del backend") }
   }
   }
   state.photo?.preview?.let {
    Image(it.asImageBitmap(), "Foto de la cuenta", modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(MaterialTheme.shapes.medium))
   }
   AnimatedVisibility(state.busy, enter = fadeIn(tween(180)), exit = fadeOut(tween(120))) {
    Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
     LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
     Text(if (state.photo == null) "Preparando foto…" else "Leyendo la cuenta y armando el mensaje…")
    }
   }
   state.notice?.let { WarningText(it) }
   notice?.let { WarningText(it) }
   if (state.photo != null && state.reading == null && !state.busy && state.notice != null) {
    OutlinedButton(onClick = model::readPhoto, enabled = state.hasToken && !state.configBusy,
     modifier = Modifier.fillMaxWidth()) { Text("Reintentar lectura") }
    FilledTonalButton(onClick = model::beginCorrection, modifier = Modifier.fillMaxWidth()) { Text("Completar cuenta a mano") }
   }
   state.reading?.let { reading ->
    SectionTitle("02 · Revisá los importes", "Tu cuenta")
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
     Text("Cuenta en el papel: ${client.name.ifBlank { "sin nombre" }}", style = MaterialTheme.typography.titleLarge)
     TotalCard(ReviewRules.total(client))
     Text("Compará el total y los movimientos con la foto.", color = MaterialTheme.colorScheme.onSurfaceVariant)
     ReadingSummary(client, reading.warnings)
     if (state.selected in state.correctedClients) Text("Cuenta corregida a mano. El total se recalculó con tus movimientos.")
     OutlinedButton(onClick = model::beginCorrection, enabled = !contactPending,
      modifier = Modifier.fillMaxWidth()) { Text("Corregir lectura") }
    }
    val problem = state.automaticProblem
    if (problem != null) WarningText(problem)
    else {
     SectionTitle("03 · Prepará el mensaje", "¿A quién le cobrás?")
     FilledTonalButton(onClick = {
      contactSession = model.session
      contactPending = true
      try { contacts.launch(contactPickerIntent()) }
      catch (_: RuntimeException) { contactPending = false; notice = "No se pudo abrir el selector de contactos." }
     }, enabled = !contactPending, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
      Text(if (state.contact == null) "Elegir contacto" else "Cambiar contacto")
     }
     if (contactPending) Text("Esperando contacto…")
    }
   }
   state.contact?.let { contact ->
    Card(Modifier.fillMaxWidth().animateContentSize(tween(180)),
     colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Contacto seleccionado", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    Text(contact.name.ifBlank { "Sin nombre" }, style = MaterialTheme.typography.headlineSmall)
    val phone = MessageRules.internationalPhone(contact.phone)
    if (phone == null) WarningText("El teléfono no es válido para WhatsApp. Elegí otro contacto con celular del área 11.")
    else Text("WhatsApp: +$phone")
    }
    }
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
    modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp), contentPadding = PaddingValues(20.dp)) {
     Text("Confirmar y abrir WhatsApp")
    }
   Text("Comprobá la foto, el detalle y el contacto. Al continuar confirmás esta cuenta. El envío lo confirmás en WhatsApp.",
    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
   if (state.photo != null || state.busy) TextButton(onClick = { model.cancel(); notice = null },
    modifier = Modifier.fillMaxWidth()) { Text("Cancelar cuenta") }
  }
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
    SectionTitle("Revisión de la foto", "Corregir cuenta")
    Text("Compará los movimientos con la foto. Podés completar importes, cambiar tipos, agregar filas o excluir totales duplicados. No se vuelve a consultar a la IA.")
    TextButton(onClick = model::cancelCorrection) { Text("Descartar correcciones") }
    state.photo?.preview?.let { Image(it.asImageBitmap(), "Foto para corregir la cuenta",
     modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp)) }
    OutlinedTextField(correction.client.name, model::correctionName, label = { Text("Nombre en el papel (opcional)") },
     modifier = Modifier.fillMaxWidth(), singleLine = true)
    correction.client.movements.forEachIndexed { index, movement ->
     Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
      Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
       Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(movement.included, { value -> model.correctionMovement(index) { it.copy(included = value) } })
        Text("Usar movimiento ${index + 1}", modifier = Modifier.weight(1f))
       }
       Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MovementType.entries.forEach { type ->
         FilterChip(selected = movement.type == type, enabled = movement.included,
          onClick = { model.correctionMovement(index) { it.copy(type = type) } }, label = { Text(type.label) })
        }
       }
       OutlinedTextField(movement.amount, { value -> model.correctionMovement(index) { it.copy(amount = value) } },
        label = { Text("Importe ${index + 1}") }, prefix = { Text("$") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
        textStyle = MaterialTheme.typography.headlineSmall.copy(fontSize = 32.sp, lineHeight = 40.sp, fontFeatureSettings = "tnum"),
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
    TotalCard(ReviewRules.total(correction.client), label = "Total recalculado")
    val problem = AutomaticMessageRules.localProblem(correction.client)
    problem?.let { WarningText(it) }
    Text("Al guardar confirmás que los movimientos coinciden con el papel. Se usará este total local aunque la IA no haya podido calcularlo.")
    Button(onClick = { model.saveCorrection() }, enabled = problem == null,
     modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) { Text("Guardar correcciones") }
   }
  }
 }
}

@Composable private fun ReadingSummary(client: ClientReading, globalWarnings: List<String>) {
 Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
  Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
   Text("Detalle de la cuenta", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
   client.movements.filter { it.included }.forEach { movement ->
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(movement.type.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val amount = MessageRules.amount(movement.amount)?.let(::displayPesos)
    Text(if (amount == null) movement.amount.ifBlank { "Importe sin leer" }
     else "${if (movement.type == MovementType.PAGO) "−" else "+"} $ $amount",
     style = MaterialTheme.typography.titleLarge, softWrap = false,
     modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()))
    movement.reference?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    movement.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    if (movement.needsReview) WarningText("Compará este movimiento con la foto antes de confirmar.")
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
   }
   if (client.needsReview) Text("Revisá esta cuenta con la foto antes de abrir WhatsApp.")
   (globalWarnings + client.warnings).distinct().forEach { WarningText(it) }
  }
 }
}

@Composable private fun MessagePreview(message: String) {
 Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  modifier = Modifier.fillMaxWidth()) {
  Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
   Text("Mensaje listo", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
   SelectionContainer {
    Column {
     Text(message.substringBefore("```"), style = MaterialTheme.typography.bodyLarge)
     Text(message.substringAfter("```\n").removeSuffix("\n```"), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.horizontalScroll(rememberScrollState()))
    }
   }
  }
 }
}

@Composable private fun WarningText(text: String) {
 Surface(color = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer,
  shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }) {
  Text(text, modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
 }
}

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

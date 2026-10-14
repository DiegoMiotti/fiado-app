package ar.com.fiado.readings

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.com.fiado.MessageRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun contactPickerIntent(): Intent =
 Intent(Intent.ACTION_PICK).setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE)

@Composable
fun ReadingPanel(model: ReadingViewModel, apply: (AppliedReading) -> Unit,
 readContact: (Uri) -> Pair<String, String>?) {
 val state by model.state.collectAsStateWithLifecycle()
 var settings by remember { mutableStateOf(false) }
 var pickerNotice by remember { mutableStateOf<String?>(null) }
 var contactSession by remember { mutableIntStateOf(0) }
 val scope = rememberCoroutineScope()
 val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
  uri?.let { model.choosePhoto(it.toString()) }
 }
 val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
  if (result.resultCode == android.app.Activity.RESULT_OK) {
   val session = contactSession
   scope.launch {
    val contact = withContext(Dispatchers.IO) { result.data?.data?.let(readContact) }
    if (contact == null) pickerNotice = "No se pudo leer el contacto."
    else model.contact(session, contact.first, contact.second)
   }
  }
 }
 Card(modifier = Modifier.fillMaxWidth()) {
  Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
   Text("Leer papel con OCR + IA", style = MaterialTheme.typography.titleMedium)
   Text("Elegí una foto y revisá los movimientos antes de reemplazar este cobro.")
   FilledTonalButton(onClick = {
    try { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    catch (_: RuntimeException) { pickerNotice = "No se pudo abrir el selector de fotos." }
   }, enabled = !state.busy && !state.configBusy, modifier = Modifier.fillMaxWidth()) { Text("Elegir foto") }
   TextButton(onClick = { settings = true }, enabled = !state.busy) { Text("Conexión del backend") }
   if (!state.hasToken) Text("Falta configurar el token del teléfono.", style = MaterialTheme.typography.bodySmall)
   pickerNotice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
  }
 }
 if (settings) ConnectionDialog(state, model, onClose = { settings = false })
 if (state.photo != null || state.busy) {
  Dialog(onDismissRequest = model::cancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
   BackHandler { model.cancel() }
   Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
     verticalArrangement = Arrangement.spacedBy(12.dp)) {
     Text("Revisar lectura", style = MaterialTheme.typography.headlineSmall)
     Text("El cobro actual se conserva hasta que confirmes. Cancelar una consulta enviada puede consumir cuota.")
     OutlinedButton(onClick = model::cancel, modifier = Modifier.fillMaxWidth()) { Text("Cancelar lectura") }
     state.photo?.preview?.let {
      Image(it.asImageBitmap(), contentDescription = "Foto manuscrita elegida",
       modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp))
      Text("Vista reducida de la foto elegida. Comprobá que los números se lean bien.", style = MaterialTheme.typography.bodySmall)
     }
     if (state.busy) {
      CircularProgressIndicator()
      Text(if (state.photo == null) "Preparando foto…" else "Consultando al backend… Puede tardar alrededor de un minuto.")
     }
     state.notice?.let { Text(it, color = MaterialTheme.colorScheme.error) }
     if (!state.busy && state.reading == null && state.photo != null) {
      Text("La foto se enviará a ${state.url} y al proveedor de IA. Cada consulta puede consumir cuota y saldo.")
      Button(onClick = model::readPhoto, enabled = state.hasToken && !state.configBusy,
       modifier = Modifier.fillMaxWidth()) { Text("Enviar foto para lectura") }
      if (!state.hasToken) Text("Cancelá y configurá el token en Conexión del backend.")
     }
     state.reading?.let { reading ->
      reading.warnings.forEach { WarningText(it) }
      if (reading.clients.size > 1) {
       Text("Elegí el bloque que querés aplicar:")
       reading.clients.forEachIndexed { index, client ->
        OutlinedButton(onClick = { model.select(index) }, modifier = Modifier.fillMaxWidth()) {
         Text("${if (state.selected == index) "✓ " else ""}${index + 1}. ${client.name.ifBlank { "Sin nombre" }}")
        }
       }
      }
      state.client?.let { client ->
       OutlinedTextField(client.name, model::name, label = { Text("Nombre para el mensaje") },
        modifier = Modifier.fillMaxWidth(), singleLine = true, isError = client.name.isBlank())
       if (client.needsReview) WarningText("La IA marcó dudas en este cliente. Revisá el bloque completo.")
       client.warnings.forEach { WarningText(it) }
       Text("Coincidencia: ${when (client.match) { "ambiguo" -> "ambigua"; "coincidencia_exacta" -> "exacta"; else -> "sin coincidencia" }}")
       if (client.candidates.isNotEmpty()) Text("Candidatos: ${client.candidates.joinToString { it.name }}. Confirmá el contacto localmente.")
       client.movements.forEachIndexed { index, movement ->
        Card(modifier = Modifier.fillMaxWidth()) {
         Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row {
           Checkbox(movement.included, { included -> model.movement(index) { it.copy(included = included) } })
           Text("Incluir movimiento ${index + 1}", modifier = Modifier.padding(top = 12.dp))
          }
          Text(movement.reference ?: "Sin referencia")
          movement.detail?.let { Text(it) }
          if (movement.needsReview) WarningText("La IA marcó este movimiento para revisión.")
          MovementType.entries.forEach { type ->
           Row {
            RadioButton(movement.type == type, onClick = { model.movement(index) { it.copy(type = type) } }, enabled = movement.included)
            Text(type.label, modifier = Modifier.padding(top = 12.dp))
           }
          }
          OutlinedTextField(movement.amount,
           { value -> model.movement(index) { it.copy(amount = value) } }, label = { Text("Importe en pesos enteros") },
           prefix = { Text("$") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
           keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = movement.included,
           isError = movement.included && MessageRules.amount(movement.amount) == null,
           supportingText = { Text("De 1 a 999.999.999. Resolvé importes vacíos o excluí la fila.") })
         }
        }
       }
       val total = ReviewRules.total(client)
       Text("Total local: ${total?.toPlainString()?.let { "$$it" } ?: "pendiente de corregir"}", style = MaterialTheme.typography.titleLarge)
       Text("Saldo anterior + cargos − pagos. Excluí las filas que sean totales duplicados.")
       if (ReviewRules.multipleBalances(client)) WarningText("Hay varios saldos anteriores. Comprobá si alguno está duplicado y excluilo si corresponde.")
       if (total != null && client.serverTotal != null && total.compareTo(client.serverTotal) != 0)
        WarningText("El total local difiere del informado por el backend (${client.serverTotal.toPlainString()}).")
       if (total != null && total.signum() <= 0) WarningText("El cobro actual solo admite totales positivos. Esta lectura no se puede aplicar.")
       val canComplicated = ReviewRules.canUseComplicated(client)
       Row {
        Checkbox(state.complicated, model::complicated, enabled = canComplicated)
        Text("Aplicar en Complicados", modifier = Modifier.padding(top = 12.dp))
       }
       Text(if (canComplicated) "Podés conservar un saldo anterior y los cargos por separado."
        else "Se aplicará el total en modo simple. Los pagos se restan del total.")
       if (!state.complicated && total != null && total > java.math.BigDecimal("999999999"))
        WarningText("El total supera el máximo del modo simple. Complicados solo está disponible con un saldo y cargos sin pagos.")
       FilledTonalButton(onClick = {
        contactSession = model.session
        try { contactPicker.launch(contactPickerIntent()) }
        catch (_: RuntimeException) { pickerNotice = "No se pudo abrir el selector de contactos." }
       }, modifier = Modifier.fillMaxWidth()) { Text("Elegir contacto para esta lectura") }
       pickerNotice?.let { WarningText(it) }
       state.contact?.let { contact ->
        Text("Contacto elegido: ${contact.name} · ${contact.originalPhone}")
        OutlinedTextField(contact.phone, model::phone, label = { Text("Teléfono del contacto") },
         modifier = Modifier.fillMaxWidth(), singleLine = true,
         keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
         isError = MessageRules.internationalPhone(contact.phone) == null)
        MessageRules.internationalPhone(contact.phone)?.let { Text("WhatsApp: +$it") }
       }
       Row {
        Checkbox(state.confirmed, model::confirm)
        Text("Revisé la foto, el nombre, todos los movimientos y el contacto.", modifier = Modifier.padding(top = 12.dp))
       }
       Button(onClick = { model.apply(apply) }, enabled = state.application != null,
        modifier = Modifier.fillMaxWidth()) { Text("Confirmar y reemplazar cobro") }
       Text("Solo se aplica el bloque elegido. El envío de WhatsApp sigue siendo manual.")
      }
     }
    }
   }
  }
 }
}

@Composable private fun WarningText(text: String) {
 Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}

@Composable private fun ConnectionDialog(state: ReadingState, model: ReadingViewModel, onClose: () -> Unit) {
 var url by remember(state.url) { mutableStateOf(state.url) }
 // Deliberately not rememberSaveable: credentials never enter saved state.
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

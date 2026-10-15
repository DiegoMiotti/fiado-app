package ar.com.fiado.readings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun AudioDialog(onSend: (ByteArray) -> Unit, onClose: () -> Unit) {
 val context = LocalContext.current
 val lifecycle = LocalLifecycleOwner.current.lifecycle
 val recorder = remember { AudioRecording(context.applicationContext) }
 val scope = rememberCoroutineScope()
 var recording by remember { mutableStateOf(false) }
 var preparing by remember { mutableStateOf(false) }
 var audio by remember { mutableStateOf<ByteArray?>(null) }
 var error by remember { mutableStateOf<String?>(null) }
 fun stop() {
  if (!recording) return
  recording = false
  try {
   val file = recorder.finish()
   preparing = true
   scope.launch {
    try {
     audio = withContext(Dispatchers.IO) {
      try {
       require(file.length() in 1..ReadingJson.MAX_AUDIO_BYTES.toLong())
       file.readBytes()
      } finally { file.delete() }
     }
    } catch (_: Exception) { error = "No se pudo preparar la grabación. Volvé a grabar." }
    finally { preparing = false }
   }
  } catch (_: Exception) { error = "No se pudo guardar el audio. Grabá unos segundos antes de detener." }
 }
 fun start() {
  audio = null; error = null
  try { recorder.start(::stop); recording = true }
  catch (_: Exception) { error = "No se pudo acceder al micrófono. Cerrá otras grabaciones y volvé a intentar." }
 }
 val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
  if (granted) start()
  else error = "Necesitás permitir el micrófono para grabar. Podés habilitarlo en los ajustes de la app."
 }
 DisposableEffect(recorder, lifecycle) {
  val observer = LifecycleEventObserver { _, event ->
   if (event == Lifecycle.Event.ON_STOP && recording) {
    recorder.discard(); recording = false
    error = "Se descartó la grabación al salir de la pantalla. Volvé a grabar."
   }
  }
  lifecycle.addObserver(observer)
  onDispose { lifecycle.removeObserver(observer); recorder.discard() }
 }
 AlertDialog(onDismissRequest = onClose, title = { Text("Grabar cuenta") },
  text = {
   Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Decí, por ejemplo: Leo me debe 4000 de la cerveza más 300 de las salchichas.")
    Text("Se enviará el archivo de audio original al backend y al modelo de IA para interpretar la cuenta. Cada lectura puede consumir cuota y saldo.")
    Text(when { recording -> "Grabando… Máximo 60 segundos."
     preparing -> "Preparando audio…"
     audio != null -> "Audio listo para enviar a la IA."
     else -> "El audio se envía cuando tocás Enviar audio a IA." })
    FilledTonalButton(onClick = {
     if (recording) stop()
     else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) start()
     else permission.launch(Manifest.permission.RECORD_AUDIO)
    }, enabled = !preparing, modifier = Modifier.fillMaxWidth()) {
     Text(if (recording) "Detener grabación" else if (audio == null) "Empezar a grabar" else "Volver a grabar")
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    Text("La IA devolverá los importes y sus detalles para revisar. Si ya cargaste una foto, se conserva como referencia.",
     style = MaterialTheme.typography.bodySmall)
   }
  },
  confirmButton = {
   TextButton(onClick = { audio?.let(onSend) }, enabled = audio != null && !recording && !preparing) {
    Text("Enviar audio a IA")
   }
  },
  dismissButton = { TextButton(onClick = onClose) { Text("Cancelar") } })
}

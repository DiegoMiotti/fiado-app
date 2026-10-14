package ar.com.fiado.readings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReadingState(
 val url: String = DEFAULT_BACKEND, val hasToken: Boolean = false,
 val configBusy: Boolean = true, val busy: Boolean = false,
 val photo: PreparedPhoto? = null, val reading: Reading? = null, val selected: Int = 0,
 val contact: ReviewContact? = null, val complicated: Boolean = false, val confirmed: Boolean = false,
 val notice: String? = null, val settingsNotice: String? = null
) {
 val client get() = reading?.clients?.getOrNull(selected)
 val application get() = client?.let { ReviewRules.application(it, contact, complicated, confirmed) }
}

class ReadingViewModel(
 private val storage: ConnectionStorage, private val gateway: ReadingGateway,
 private val photos: PhotoPreparation, private val io: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
 private val mutable = MutableStateFlow(ReadingState())
 val state = mutable.asStateFlow()
 private var work: Job? = null
 private var generation = 0
 val session get() = generation
 init {
  viewModelScope.launch {
   try {
    val connection = withContext(io) { storage.read() }
    mutable.value = mutable.value.copy(url = connection.url, hasToken = connection.token.isNotBlank(), configBusy = false)
   } catch (_: Exception) {
    mutable.value = mutable.value.copy(configBusy = false, settingsNotice = "No se pudo leer la credencial. Borrala y volvé a guardarla.")
   }
  }
 }
 fun saveConnection(url: String, replacement: String) {
  if (state.value.configBusy || state.value.busy) return
  val validated = runCatching { backendUrl(url) }.getOrNull()
  if (validated == null || replacement.any { it.isWhitespace() || it.code < 32 } || replacement.length > 4096) {
   mutable.value = state.value.copy(settingsNotice = "Revisá la URL HTTPS y el token sin espacios.")
   return
  }
  mutable.value = state.value.copy(configBusy = true, settingsNotice = null)
  viewModelScope.launch {
   try {
    val connection = withContext(io) {
     // Never silently send an existing credential to a different server.
     val old = if (replacement.isEmpty()) storage.read() else Connection()
     require(replacement.isNotEmpty() || validated == old.url) { "Al cambiar de servidor, ingresá un token nuevo." }
     Connection(validated, replacement.ifEmpty { old.token }).also { storage.write(it) }
    }
    mutable.value = state.value.copy(url = connection.url, hasToken = connection.token.isNotBlank(), configBusy = false,
     settingsNotice = "Conexión guardada.")
   } catch (_: Exception) {
    mutable.value = state.value.copy(configBusy = false, settingsNotice = "No se pudo guardar. Si cambiás de servidor, ingresá un token nuevo; si la credencial está dañada, borrala primero.")
   }
  }
 }
 fun deleteConnection() {
  if (state.value.configBusy || state.value.busy) return
  mutable.value = state.value.copy(configBusy = true)
  viewModelScope.launch {
   try {
    withContext(io) { storage.delete() }
    mutable.value = state.value.copy(url = DEFAULT_BACKEND, hasToken = false, configBusy = false, settingsNotice = "Credencial borrada.")
   } catch (_: Exception) {
    mutable.value = state.value.copy(configBusy = false, settingsNotice = "No se pudo borrar la credencial.")
   }
  }
 }
 fun choosePhoto(uri: String) {
  if (state.value.busy) return
  cancel()
  mutable.value = state.value.copy(busy = true)
  work = viewModelScope.launch {
   try {
    val photo = photos.prepare(uri)
    mutable.value = state.value.copy(photo = photo, busy = false)
   } catch (error: CancellationException) { throw error }
   catch (_: Exception) { mutable.value = state.value.copy(busy = false, notice = "No se pudo preparar la foto. Elegí una imagen JPEG, PNG o WebP válida.") }
  }
 }
 fun readPhoto() {
  val current = state.value
  if (current.busy || current.configBusy || current.reading != null || current.photo == null) return
  if (!current.hasToken) {
   mutable.value = current.copy(notice = "Ingresá el token del teléfono en Conexión antes de consultar.")
   return
  }
  mutable.value = current.copy(busy = true, notice = null)
  work = viewModelScope.launch {
   try {
    val connection = withContext(io) { storage.read() }
    val reading = withTimeout(65_000) { gateway.read(connection, current.photo.jpeg) }
    mutable.value = state.value.copy(reading = reading, busy = false)
   } catch (_: TimeoutCancellationException) {
    mutable.value = state.value.copy(busy = false, notice = "Se agotó el tiempo de espera. La consulta puede haber consumido cuota; no se reintentó.")
   } catch (error: CancellationException) { throw error }
   catch (error: Exception) {
    mutable.value = state.value.copy(busy = false, notice = (error as? ReadingFailure)?.notice ?: "No se pudo completar la lectura. El cobro actual se conserva.")
   }
  }
 }
 fun select(index: Int) {
  if (state.value.reading?.clients?.getOrNull(index) == null) return
  generation++
  mutable.value = state.value.copy(selected = index, contact = null, confirmed = false, complicated = false)
 }
 private fun edit(transform: (ClientReading) -> ClientReading) {
  val current = state.value
  val reading = current.reading ?: return
  val clients = reading.clients.toMutableList()
  clients[current.selected] = transform(clients[current.selected])
  mutable.value = current.copy(reading = reading.copy(clients = clients), confirmed = false,
   complicated = current.complicated && ReviewRules.canUseComplicated(clients[current.selected]))
 }
 fun name(value: String) = edit { it.copy(name = value) }
 fun movement(index: Int, transform: (Movement) -> Movement) = edit { client ->
  client.copy(movements = client.movements.mapIndexed { i, movement -> if (i == index) transform(movement) else movement })
 }
 fun contact(session: Int, name: String, rawPhone: String) {
  if (session != generation || state.value.client == null) return
  mutable.value = state.value.copy(contact = ReviewContact(name, rawPhone,
   ar.com.fiado.MessageRules.internationalPhone(rawPhone)?.let { "+$it" } ?: rawPhone), confirmed = false)
 }
 fun phone(value: String) {
  mutable.value = state.value.copy(contact = state.value.contact?.copy(phone = value), confirmed = false)
 }
 fun complicated(value: Boolean) {
  mutable.value = state.value.copy(complicated = value && state.value.client?.let { ReviewRules.canUseComplicated(it) } == true, confirmed = false)
 }
 fun confirm(value: Boolean) { mutable.value = state.value.copy(confirmed = value) }
 fun apply(consumer: (AppliedReading) -> Unit) {
  val application = state.value.application ?: return
  consumer(application)
  cancel()
 }
 fun cancel() {
  generation++
  work?.cancel()
  mutable.value = state.value.copy(photo = null, reading = null, selected = 0, contact = null,
   busy = false, confirmed = false, complicated = false, notice = null)
 }
}

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

data class ReadingCorrection(val index: Int, val client: ClientReading)

data class ReadingState(
 val url: String = DEFAULT_BACKEND, val hasToken: Boolean = false,
 val configBusy: Boolean = true, val busy: Boolean = false,
 val photo: PreparedPhoto? = null, val reading: Reading? = null, val selected: Int = 0,
 val selectedExplicitly: Boolean = false,
 val correction: ReadingCorrection? = null, val correctedClients: Set<Int> = emptySet(),
 val contact: ReviewContact? = null, val complicated: Boolean = false, val confirmed: Boolean = false,
 val notice: String? = null, val settingsNotice: String? = null
) {
 val client get() = reading?.clients?.getOrNull(selected)
 val application get() = client?.let { ReviewRules.application(it, contact, complicated, confirmed) }
 val automaticProblem get() = reading?.let { AutomaticMessageRules.problem(it, selected, selectedExplicitly, selected in correctedClients) }
 fun automaticDraft(clock: java.time.Clock = java.time.Clock.systemDefaultZone()): WhatsAppDraft? =
  reading?.let { AutomaticMessageRules.draft(it, selected, selectedExplicitly, contact, clock, selected in correctedClients) }
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
 fun choosePhoto(uri: String, autoRead: Boolean = false) {
  if (state.value.busy) return
  cancel()
  mutable.value = state.value.copy(busy = true)
  work = viewModelScope.launch {
   try {
    val photo = photos.prepare(uri)
    mutable.value = state.value.copy(photo = photo, busy = autoRead)
    if (autoRead) executeReading(photo)
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
  work = viewModelScope.launch { executeReading(current.photo) }
 }
 private suspend fun executeReading(photo: PreparedPhoto) {
  if (!state.value.hasToken || state.value.configBusy) {
   mutable.value = state.value.copy(busy = false, notice = "Ingresá el token del teléfono en Conexión antes de consultar.")
   return
  }
  mutable.value = state.value.copy(busy = true, notice = null)
   try {
    val connection = withContext(io) { storage.read() }
    val reading = withTimeout(65_000) { gateway.read(connection, photo.jpeg) }
    mutable.value = state.value.copy(reading = reading, busy = false, selected = 0,
     selectedExplicitly = reading.clients.size == 1, contact = null, correctedClients = emptySet(), correction = null)
   } catch (_: TimeoutCancellationException) {
    mutable.value = state.value.copy(busy = false, notice = "Se agotó el tiempo de espera. La consulta puede haber consumido cuota; no se reintentó.")
   } catch (error: CancellationException) { throw error }
   catch (error: Exception) {
    mutable.value = state.value.copy(busy = false, notice = (error as? ReadingFailure)?.notice ?: "No se pudo completar la lectura. El cobro actual se conserva.")
   }
 }
 fun select(index: Int) {
  if (state.value.reading?.clients?.getOrNull(index) == null) return
  generation++
  mutable.value = state.value.copy(selected = index, selectedExplicitly = true, contact = null, confirmed = false,
   complicated = false, correction = null)
 }
 fun beginCorrection() {
  val current = state.value
  if (current.busy || current.photo == null || current.correction != null) return
  if (current.reading != null && !current.selectedExplicitly) return
  val client = current.client ?: ClientReading("", listOf(Movement(MovementType.CARGO, null, null, "", true)),
   emptyList(), true, "sin_coincidencia", emptyList(), null)
  mutable.value = current.copy(correction = ReadingCorrection(if (current.reading == null) -1 else current.selected, client))
 }
 private fun editCorrection(transform: (ClientReading) -> ClientReading) {
  val correction = state.value.correction ?: return
  mutable.value = state.value.copy(correction = correction.copy(client = transform(correction.client)))
 }
 fun correctionName(value: String) = editCorrection { it.copy(name = value) }
 fun correctionMovement(index: Int, transform: (Movement) -> Movement) = editCorrection { client ->
  client.copy(movements = client.movements.mapIndexed { i, movement -> if (i == index) transform(movement) else movement })
 }
 fun addCorrectionMovement() = editCorrection {
  it.copy(movements = it.movements + Movement(MovementType.CARGO, null, null, "", true))
 }
 fun cancelCorrection() { mutable.value = state.value.copy(correction = null) }
 fun saveCorrection(): Boolean {
  val current = state.value
  val correction = current.correction ?: return false
  if (AutomaticMessageRules.localProblem(correction.client) != null) return false
  val index = if (correction.index < 0) 0 else correction.index
  val reading = if (correction.index < 0) Reading("manual", listOf(correction.client), emptyList())
   else current.reading?.let { reading ->
    reading.copy(clients = reading.clients.mapIndexed { i, client -> if (i == index) correction.client else client })
   } ?: return false
  generation++ // Invalidate any contact result that belonged to a previous account snapshot.
  mutable.value = current.copy(reading = reading, selected = index, selectedExplicitly = true,
   correction = null, correctedClients = current.correctedClients + index, notice = null, confirmed = false)
  return true
 }
 private fun edit(transform: (ClientReading) -> ClientReading) {
  val current = state.value
  val reading = current.reading ?: return
  val clients = reading.clients.toMutableList()
  clients[current.selected] = transform(clients[current.selected])
  mutable.value = current.copy(reading = reading.copy(clients = clients), confirmed = false,
   correctedClients = current.correctedClients - current.selected,
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
  mutable.value = state.value.copy(photo = null, reading = null, selected = 0, selectedExplicitly = false, contact = null,
   busy = false, confirmed = false, complicated = false, notice = null, correction = null, correctedClients = emptySet())
 }
}

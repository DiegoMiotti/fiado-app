package ar.com.fiado

import androidx.lifecycle.SavedStateHandle
import ar.com.fiado.readings.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingViewModelTest {
 private val dispatcher = StandardTestDispatcher()
 @Before fun setup() { Dispatchers.setMain(dispatcher) }
 @After fun cleanup() { Dispatchers.resetMain() }
 private class Store(var connection: Connection = Connection(token = "fake-test-token")) : ConnectionStorage {
  override fun read() = connection
  override fun write(connection: Connection) { this.connection = connection }
  override fun delete() { connection = Connection() }
 }
 private val photos = object : PhotoPreparation {
  override suspend fun prepare(uri: String) = PreparedPhoto(null, byteArrayOf(1, 2, 3))
 }
 private fun model(store: Store = Store(), gateway: ReadingGateway = object : ReadingGateway {
  override suspend fun read(connection: Connection, jpeg: ByteArray) = ReadingJson.parse(sampleReading)
 }) = ReadingViewModel(store, gateway, photos, dispatcher)
 @Test fun rawAudioIsSentOnceAndAiDetailsAppearBesidePrices() = runTest(dispatcher) {
  var calls = 0
  val bytes = byteArrayOf(1, 2, 3, 4)
  val response = CompletableDeferred<Reading>()
  val account = client(movement(MovementType.CARGO, "4000").copy(detail = "cerveza"),
   movement(MovementType.CARGO, "300").copy(detail = "salchichas")).copy(serverTotal = java.math.BigDecimal("4300"))
  val model = model(gateway = object : ReadingGateway {
   override suspend fun read(connection: Connection, jpeg: ByteArray) = ReadingJson.parse(sampleReading)
   override suspend fun readAudio(connection: Connection, audio: ByteArray): Reading {
    calls++; assertArrayEquals(bytes, audio); return response.await()
   }
  })
  advanceUntilIdle()
  model.choosePhoto("fixture", autoRead = true); advanceUntilIdle()
  val photo = model.state.value.photo
  model.readAudio(bytes); runCurrent()
  model.readAudio(bytes); runCurrent()
  assertEquals(1, calls)
  assertTrue(model.state.value.busy)
  response.complete(Reading("audio", listOf(account), emptyList())); advanceUntilIdle()
  assertSame(photo, model.state.value.photo)
  assertNull(model.state.value.contact)
  model.contact(model.session, "Leo", "23456789")
  val message = model.state.value.automaticDraft()!!.message
  assertTrue(message.contains("4.000  cerveza"))
  assertTrue(message.contains("300  salchichas"))
  assertTrue(message.contains("TOTAL  $ 4.300"))
 }
 @Test fun missingTokenAndInvalidAudioNeverCallBackend() = runTest(dispatcher) {
  val gateway = object : ReadingGateway {
   override suspend fun read(connection: Connection, jpeg: ByteArray): Reading = error("Unexpected photo")
   override suspend fun readAudio(connection: Connection, audio: ByteArray): Reading = error("Unexpected audio")
  }
  val missingToken = model(Store(Connection()), gateway)
  val ready = model(gateway = gateway)
  advanceUntilIdle()
  missingToken.readAudio(byteArrayOf(1)); advanceUntilIdle()
  assertTrue(missingToken.state.value.notice!!.contains("token"))
  ready.readAudio(byteArrayOf()); ready.readAudio(ByteArray(ReadingJson.MAX_AUDIO_BYTES + 1)); advanceUntilIdle()
  assertNull(ready.state.value.audio)
 }
 @Test fun failedAudioPreservesAccountAndCancellationDropsLateResult() = runTest(dispatcher) {
  val response = CompletableDeferred<Reading>()
  var fail = true
  val model = model(gateway = object : ReadingGateway {
   override suspend fun read(connection: Connection, jpeg: ByteArray) = ReadingJson.parse(sampleReading)
   override suspend fun readAudio(connection: Connection, audio: ByteArray): Reading {
    if (fail) throw ReadingFailure("Audio falló")
    return response.await()
   }
  })
  advanceUntilIdle()
  model.choosePhoto("fixture", autoRead = true); advanceUntilIdle()
  model.contact(model.session, "Anterior", "23456789")
  val original = model.state.value
  model.readAudio(byteArrayOf(1)); advanceUntilIdle()
  assertEquals(original.reading, model.state.value.reading)
  assertEquals(original.contact, model.state.value.contact)
  assertEquals("Audio falló", model.state.value.notice)
  fail = false
  model.readAudio(); runCurrent()
  model.cancel(); response.complete(ReadingJson.parse(sampleReading)); advanceUntilIdle()
  assertNull(model.state.value.reading)
  assertNull(model.state.value.audio)
  assertFalse(model.state.value.busy)
 }
 @Test fun noCredentialNeverCallsBackend() = runTest(dispatcher) {
  var calls = 0
  val model = model(Store(Connection()), object : ReadingGateway {
   override suspend fun read(connection: Connection, jpeg: ByteArray): Reading { calls++; error("Must not call") }
  })
  advanceUntilIdle()
  model.choosePhoto("fake-photo"); advanceUntilIdle()
  model.readPhoto(); advanceUntilIdle()
  assertEquals(0, calls)
  assertTrue(model.state.value.notice!!.contains("token"))
 }
 @Test fun confirmingPhotoAutomaticallyReadsExactlyOnceAndCreatesDetailedDraft() = runTest(dispatcher) {
  var calls = 0
  val response = CompletableDeferred<Reading>()
  val model = model(gateway = object : ReadingGateway {
   override suspend fun read(connection: Connection, jpeg: ByteArray): Reading { calls++; return response.await() }
  })
  advanceUntilIdle()
  model.choosePhoto("fixture", autoRead = true); runCurrent()
  model.readPhoto(); model.choosePhoto("double-tap", autoRead = true); runCurrent()
  assertEquals(1, calls)
  response.complete(ReadingJson.parse(sampleReading)); advanceUntilIdle()
  assertNull(model.state.value.automaticDraft())
  model.contact(model.session, "Leonardo", "23456789")
  val draft = model.state.value.automaticDraft()!!
  assertTrue(draft.message.contains("```"))
  assertTrue(draft.message.contains("- Pago"))
  assertTrue(draft.message.contains("11.000"))
  assertFalse(model.state.value.confirmed) // Final WhatsApp tap is the confirmation in this flow.
 }
 @Test fun automaticCaptureWithoutCredentialDoesNotConsultAndCanBeCancelled() = runTest(dispatcher) {
  var calls = 0
  val model = model(Store(Connection()), object : ReadingGateway {
   override suspend fun read(connection: Connection, jpeg: ByteArray): Reading { calls++; error("Must not call") }
  })
  advanceUntilIdle(); model.choosePhoto("fixture", autoRead = true); advanceUntilIdle()
  assertEquals(0, calls)
  assertFalse(model.state.value.busy)
  assertTrue(model.state.value.notice!!.contains("token"))
  model.cancel()
  assertNull(model.state.value.automaticDraft())
  assertNull(model.state.value.photo)
 }
 @Test fun duplicateRequestsAreIgnoredAndCancellationDiscardsResults() = runTest(dispatcher) {
  var calls = 0
  val response = CompletableDeferred<Reading>()
  val model = model(gateway = object : ReadingGateway {
   override suspend fun read(connection: Connection, jpeg: ByteArray): Reading { calls++; return response.await() }
  })
  advanceUntilIdle(); model.choosePhoto("fake"); advanceUntilIdle()
  model.readPhoto(); model.readPhoto(); runCurrent()
  assertEquals(1, calls)
  assertTrue(model.state.value.busy)
  model.cancel(); response.complete(ReadingJson.parse(sampleReading)); advanceUntilIdle()
  assertNull(model.state.value.reading)
  assertNull(model.state.value.photo)
  assertFalse(model.state.value.busy)
 }
 @Test fun failedOrCancelledReadingNeverTouchesCurrentDraft() = runTest(dispatcher) {
  val fiado = FiadoViewModel(SavedStateHandle())
  fiado.amount("2500"); fiado.contact("Anterior", "23456789")
  val model = model(gateway = object : ReadingGateway {
   override suspend fun read(connection: Connection, jpeg: ByteArray): Reading = throw ReadingFailure("Error simulado")
  })
  advanceUntilIdle(); model.choosePhoto("fake"); advanceUntilIdle(); model.readPhoto(); advanceUntilIdle()
  model.apply(fiado::applyReading)
  assertEquals("Error simulado", model.state.value.notice)
  model.cancel()
  assertEquals("2500", fiado.amount.value)
  assertEquals("Anterior", fiado.name.value)
 }
 @Test fun timeoutEndsLoadingWithoutRetrying() = runTest(dispatcher) {
  var calls = 0
  val model = model(gateway = object : ReadingGateway {
   override suspend fun read(connection: Connection, jpeg: ByteArray): Reading {
    calls++; delay(70_000); return ReadingJson.parse(sampleReading)
   }
  })
  advanceUntilIdle(); model.choosePhoto("fake"); advanceUntilIdle(); model.readPhoto(); advanceUntilIdle()
  assertEquals(1, calls)
  assertFalse(model.state.value.busy)
  assertNull(model.state.value.reading)
  assertTrue(model.state.value.notice!!.contains("tiempo"))
 }
 @Test fun editsInvalidateConfirmationAndApplicationReplacesOldFields() = runTest(dispatcher) {
  val fiado = FiadoViewModel(SavedStateHandle())
  fiado.complicated(true); fiado.previous("9000"); fiado.addition(0, "8000")
  val model = model()
  advanceUntilIdle(); model.choosePhoto("fake"); advanceUntilIdle(); model.readPhoto(); advanceUntilIdle()
  model.contact(model.session, "Leonardo", "23456789")
  model.confirm(true)
  model.movement(0) { it.copy(amount = "14000") }
  assertFalse(model.state.value.confirmed)
  model.apply(fiado::applyReading)
  assertEquals("9000", fiado.previous.value)
  model.confirm(true); model.apply(fiado::applyReading)
  assertEquals("12.000", fiado.amount.value)
  assertFalse(fiado.complicated.value)
  assertEquals("", fiado.previous.value)
  assertEquals(listOf(""), fiado.additions.value)
  assertEquals("Leo", fiado.name.value)
  assertEquals("+5491123456789", fiado.phone.value)
  assertNull(model.state.value.photo)
 }
 @Test fun staleContactDoesNotCrossBlocksAndSwitchResetsConfirmation() = runTest(dispatcher) {
  val reading = ReadingJson.parse(sampleReading).let { it.copy(clients = it.clients + it.clients.single().copy(name = "Otro")) }
  val model = model(gateway = object : ReadingGateway { override suspend fun read(connection: Connection, jpeg: ByteArray) = reading })
  advanceUntilIdle(); model.choosePhoto("fake"); advanceUntilIdle(); model.readPhoto(); advanceUntilIdle()
  val oldSession = model.session
  model.contact(oldSession, "Leo", "23456789"); model.confirm(true); model.select(1)
  model.contact(oldSession, "Leo", "23456789")
  assertNull(model.state.value.contact)
  assertFalse(model.state.value.confirmed)
 }
 @Test fun connectionCanBeReplacedDeletedAndNotReusedOnAnotherServer() = runTest(dispatcher) {
  val store = Store()
  val model = model(store)
  advanceUntilIdle()
  model.saveConnection("https://other.example", ""); advanceUntilIdle()
  assertEquals(DEFAULT_BACKEND, store.connection.url)
  model.saveConnection("https://other.example", "new-fake-token"); advanceUntilIdle()
  assertEquals("new-fake-token", store.connection.token)
  model.deleteConnection(); advanceUntilIdle()
  assertFalse(model.state.value.hasToken)
  assertEquals("", store.connection.token)
 }
 @Test fun correctionsAreIsolatedUntilSavedAndCancellationPreservesOriginal() = runTest(dispatcher) {
  val model = model()
  advanceUntilIdle(); model.choosePhoto("fixture", autoRead = true); advanceUntilIdle()
  model.contact(model.session, "Leonardo", "23456789")
  val original = model.state.value.automaticDraft()!!
  model.beginCorrection()
  model.correctionMovement(0) { it.copy(amount = "14000") }
  assertEquals("13000", model.state.value.client!!.movements[0].amount)
  model.cancelCorrection()
  assertEquals(original, model.state.value.automaticDraft())
  model.beginCorrection(); model.correctionMovement(0) { it.copy(amount = "14000") }
  assertTrue(model.saveCorrection())
  assertEquals("14000", model.state.value.client!!.movements[0].amount)
  assertTrue(model.state.value.automaticDraft()!!.message.contains("12.000"))
  assertNull(model.state.value.correction)
  assertTrue(0 in model.state.value.correctedClients)
  model.choosePhoto("another", autoRead = true); advanceUntilIdle()
  assertTrue(model.state.value.correctedClients.isEmpty())
 }
 @Test fun failedReadingCanBeCompletedLocallyWithoutAnotherBackendCall() = runTest(dispatcher) {
  var calls = 0
  val model = model(gateway = object : ReadingGateway {
   override suspend fun read(connection: Connection, jpeg: ByteArray): Reading {
    calls++; throw ReadingFailure("Lectura ficticia fallida")
   }
  })
  advanceUntilIdle(); model.choosePhoto("fixture", autoRead = true); advanceUntilIdle()
  model.beginCorrection()
  assertFalse(model.saveCorrection())
  model.correctionMovement(0) { it.copy(amount = "5000") }
  model.addCorrectionMovement()
  model.correctionMovement(1) { it.copy(type = MovementType.PAGO, amount = "2000") }
  assertTrue(model.saveCorrection())
  model.contact(model.session, "Leonardo", "23456789")
  val draft = model.state.value.automaticDraft()!!
  assertTrue(draft.message.contains("TOTAL  $ 3.000"))
  assertTrue(draft.message.contains("- Pago"))
  assertEquals(1, calls)
  assertNull(model.state.value.notice)
 }
 @Test fun manualCorrectionsCannotAuthorizeOtherClientBlocks() = runTest(dispatcher) {
  val reading = ReadingJson.parse(sampleReading).let {
   val unknown = it.clients.single().copy(serverTotal = null)
   it.copy(clients = listOf(unknown, unknown.copy(name = "Otro")))
  }
  val model = model(gateway = object : ReadingGateway { override suspend fun read(connection: Connection, jpeg: ByteArray) = reading })
  advanceUntilIdle(); model.choosePhoto("fixture", autoRead = true); advanceUntilIdle()
  model.beginCorrection(); assertNull(model.state.value.correction)
  model.select(0); model.beginCorrection(); assertTrue(model.saveCorrection())
  model.contact(model.session, "Leo", "23456789")
  assertNotNull(model.state.value.automaticDraft())
  model.select(1); model.contact(model.session, "Otro", "23456789")
  assertNull(model.state.value.automaticDraft())
  assertTrue(0 in model.state.value.correctedClients)
  assertFalse(1 in model.state.value.correctedClients)
 }
 @Test fun complicatedApplicationKeepsExactBreakdownAndRejectsInvalidBeforeMutation() {
  val model = FiadoViewModel(SavedStateHandle())
  model.amount("2000")
  val valid = ReviewRules.application(client(movement(MovementType.SALDO, "10000"), movement(MovementType.CARGO, "5000")), reviewContact, true, true)!!
  model.applyReading(valid)
  assertTrue(model.complicated.value)
  assertEquals("10.000", model.previous.value)
  assertEquals(listOf("5.000"), model.additions.value)
  assertEquals("", model.amount.value)
  assertTrue(runCatching { model.applyReading(valid.copy(total = "999")) }.isFailure)
  assertEquals("10.000", model.previous.value)
 }
}

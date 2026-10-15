package ar.com.fiado

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import ar.com.fiado.readings.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class FiadoDeviceTest {
 @get:Rule val compose = createComposeRule()
 private lateinit var model: ReadingViewModel
 private var opened: WhatsAppDraft? = null
 private var calls = 0
 private fun show(token: String = "fixture-only", amount: String = "4200", serverTotal: BigDecimal? = BigDecimal("11200"),
  failReading: Boolean = false, fontScale: Float = 1f) {
  compose.runOnUiThread {
   model = ReadingViewModel(object : ConnectionStorage {
    override fun read() = Connection(token = token)
    override fun write(connection: Connection) = Unit
    override fun delete() = Unit
   }, object : ReadingGateway {
    override suspend fun read(connection: Connection, jpeg: ByteArray): Reading {
     calls++
     if (failReading) throw ReadingFailure("Lectura ficticia fallida")
     return Reading("fixture", listOf(ClientReading("Leo", listOf(
      Movement(MovementType.SALDO, null, null, "10000", false),
      Movement(MovementType.CARGO, "lunes", null, amount, false),
      Movement(MovementType.PAGO, null, null, "3000", false)
     ), emptyList(), false, "sin_coincidencia", emptyList(), serverTotal)), emptyList())
    }
   }, object : PhotoPreparation {
    override suspend fun prepare(uri: String) = PreparedPhoto(null, byteArrayOf(1))
   })
  }
  compose.setContent {
   val density = LocalDensity.current
   CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) { FiadoTheme {
   ReadingPanel(model, { phone, text -> opened = WhatsAppDraft(phone, text); true }, { null })
  } } }
  compose.waitUntil(5000) { !model.state.value.configBusy }
 }
 private fun readPhoto() {
  compose.runOnIdle { model.choosePhoto("fixture", autoRead = true) }
  compose.waitUntil(5000) { model.state.value.reading != null && !model.state.value.busy }
 }
 @Test fun startsWithPhotoAndNoAmountFieldsOrModeSwitch() {
  show()
  compose.onNodeWithText("Sacar foto").assertIsEnabled()
  compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
  compose.onAllNodes(isToggleable()).assertCountEquals(0)
  compose.onNodeWithText("Confirmar y abrir WhatsApp").performScrollTo().assertIsNotEnabled()
 }
 @Test fun missingTokenBlocksCaptureAndSharing() {
  show(token = "")
  compose.onNodeWithText("Sacar foto").assertIsNotEnabled()
  compose.onNodeWithText("Confirmar y abrir WhatsApp").performScrollTo().assertIsNotEnabled()
  assertEquals(0, calls)
 }
 @Test fun photoAndContactProduceDetailedMessageWithoutTypingOrApplyingToManualForm() {
  show(); readPhoto()
  compose.runOnIdle { model.contact(model.session, "Leonardo", "23456789") }
  compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
  compose.onNodeWithText("Mensaje listo").performScrollTo().assertExists()
  compose.onNodeWithText("Confirmar y abrir WhatsApp").performScrollTo().assertIsEnabled().performClick()
  compose.runOnIdle {
   val draft = requireNotNull(opened)
   assertEquals("+5491123456789", draft.phone)
   assertTrue(draft.message.contains("Leonardo"))
   assertTrue(draft.message.contains("- Pago"))
   assertTrue(draft.message.contains("TOTAL  $ 11.200"))
   assertEquals(1, calls)
  }
 }
 @Test fun unclearAmountBlocksSharingAndOffersAnotherPhoto() {
  show(amount = "", serverTotal = null); readPhoto()
  compose.onNodeWithText("Sacar otra foto").performScrollTo().assertIsEnabled()
  compose.onNodeWithText("Elegir contacto").assertDoesNotExist()
  compose.onNodeWithText("Confirmar y abrir WhatsApp").performScrollTo().assertIsNotEnabled()
  assertNull(opened)
 }
 @Test fun cancellingAccountRemovesContactAndGeneratedMessage() {
  show(); readPhoto()
  compose.runOnIdle { model.contact(model.session, "Leonardo", "23456789") }
  compose.onNodeWithText("Cancelar cuenta").performScrollTo().performClick()
  compose.onNodeWithText("Mensaje listo").assertDoesNotExist()
  compose.onNodeWithText("Confirmar y abrir WhatsApp").performScrollTo().assertIsNotEnabled()
  compose.runOnIdle { assertNull(model.state.value.contact); assertNull(model.state.value.reading) }
 }
 @Test fun invalidContactCannotOpenWhatsApp() {
  show(); readPhoto()
  compose.runOnIdle { model.contact(model.session, "Contacto", "123") }
  compose.onNodeWithText("Confirmar y abrir WhatsApp").performScrollTo().assertIsNotEnabled()
  assertNull(opened)
 }
 @Test fun unreadAmountCanBeCorrectedAndSharedWithoutAnotherQuery() {
  show(amount = "", serverTotal = null); readPhoto()
  compose.onNodeWithText("Corregir lectura").performScrollTo().performClick()
  compose.onNodeWithText("Guardar correcciones").performScrollTo().assertIsNotEnabled()
  compose.onNodeWithText("Importe 2").performScrollTo().performTextReplacement("4200")
  compose.onNodeWithText("Guardar correcciones").performScrollTo().assertIsEnabled().performClick()
  compose.runOnIdle { model.contact(model.session, "Leonardo", "23456789") }
  compose.onNodeWithText("Confirmar y abrir WhatsApp").performScrollTo().assertIsEnabled().performClick()
  compose.runOnIdle { assertTrue(opened!!.message.contains("11.200")); assertEquals(1, calls) }
 }
 @Test fun discardingCorrectionsKeepsThePreviousMessage() {
  show(); readPhoto()
  compose.runOnIdle { model.contact(model.session, "Leonardo", "23456789") }
  compose.onNodeWithText("Corregir lectura").performScrollTo().performClick()
  compose.onNodeWithText("Importe 2").performScrollTo().performTextReplacement("9999")
  compose.onNodeWithText("Descartar correcciones").performScrollTo().performClick()
  compose.onNodeWithText("Confirmar y abrir WhatsApp").performScrollTo().assertIsEnabled().performClick()
  compose.runOnIdle { assertTrue(opened!!.message.contains("11.200")); assertFalse(opened!!.message.contains("9.999")) }
 }
 @Test fun failedAiReadingOffersManualCompletionFromThePhoto() {
  show(failReading = true)
  compose.runOnIdle { model.choosePhoto("fixture", autoRead = true) }
  compose.waitUntil(5000) { !model.state.value.busy && model.state.value.notice != null }
  compose.onNodeWithText("Completar cuenta a mano").performScrollTo().performClick()
  compose.onNodeWithText("Importe 1").performScrollTo().performTextReplacement("1500")
  compose.onNodeWithText("Guardar correcciones").performScrollTo().assertIsEnabled().performClick()
  compose.runOnIdle { model.contact(model.session, "Leonardo", "23456789") }
  compose.onNodeWithText("Confirmar y abrir WhatsApp").performScrollTo().assertIsEnabled().performClick()
  compose.runOnIdle { assertTrue(opened!!.message.contains("1.500")); assertEquals(1, calls) }
 }
 @Test fun largeFontKeepsTotalContactAndWhatsAppActionReachable() {
  show(fontScale = 2f); readPhoto()
  compose.runOnIdle { model.contact(model.session, "Leonardo con un apellido largo", "23456789") }
  compose.onNodeWithContentDescription("11.200 pesos").performScrollTo().assertIsDisplayed()
  compose.onNodeWithText("Leonardo con un apellido largo").performScrollTo().assertIsDisplayed()
  compose.onNodeWithText("Confirmar y abrir WhatsApp").performScrollTo().assertIsEnabled().performClick()
  compose.runOnIdle { assertTrue(opened!!.message.contains("11.200")); assertEquals(1, calls) }
 }
}

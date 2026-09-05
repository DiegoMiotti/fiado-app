package ar.com.fiado

import android.content.ClipboardManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FiadoDeviceTest {
 @get:Rule val compose = createAndroidComposeRule<MainActivity>()

 @Test fun generatesAndCopiesWithoutContact() {
  compose.onNodeWithText("Importe de hoy").performTextInput("1500,50")
  compose.onNodeWithText("Copiar mensaje").performScrollTo().assertIsEnabled().performClick()
  compose.waitUntil(timeoutMillis = 5000) {
   compose.activity.hasWindowFocus() &&
    compose.activity.getSystemService(ClipboardManager::class.java).hasPrimaryClip()
  }
  compose.runOnIdle {
   val text = compose.activity.getSystemService(ClipboardManager::class.java).primaryClip?.getItemAt(0)?.text.toString()
   assertEquals(MessageRules.message("1500,50"), text)
  }
  compose.onNodeWithText("Abrir WhatsApp").assertIsNotEnabled()
 }

 @Test fun invalidAmountCannotBeCopied() {
  compose.onNodeWithText("Importe de hoy").performTextInput("1.500")
  compose.onNodeWithText("Copiar mensaje").performScrollTo().assertIsNotEnabled()
  compose.onNodeWithText("Abrir WhatsApp").assertIsNotEnabled()
 }

 @Test fun recreationPreservesDraftAndClearResetsIt() {
  compose.onNodeWithText("Importe de hoy").performTextInput("2500")
  compose.activityRule.scenario.recreate()
  compose.onNodeWithText("Importe de hoy").assertTextContains("2500")
  compose.onNodeWithText("Limpiar").performScrollTo().performClick()
  compose.onNodeWithText("Copiar mensaje").performScrollTo().assertIsNotEnabled()
 }
}

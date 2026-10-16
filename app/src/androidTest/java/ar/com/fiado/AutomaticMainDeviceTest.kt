package ar.com.fiado

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutomaticMainDeviceTest {
 @get:Rule val compose = createAndroidComposeRule<MainActivity>()
 @Test fun mainActivityKeepsPhotoAndDictationAfterRecreation() {
  compose.onNodeWithText("Cargar cuenta").assertExists()
  compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
  compose.activityRule.scenario.recreate()
  compose.onNodeWithText("Cargar cuenta").assertExists()
  compose.onAllNodes(hasSetTextAction()).assertCountEquals(0)
  compose.onNodeWithText("Confirmar y abrir WhatsApp").performScrollTo().assertIsNotEnabled()
 }
}

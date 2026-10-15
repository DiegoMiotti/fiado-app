package ar.com.fiado

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import ar.com.fiado.readings.TotalCard
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class AmountLayoutDeviceTest {
 @get:Rule val compose = createComposeRule()

 @Test fun longTotalAtDoubleFontScaleHasFullAccessibleValueAndCanScroll() {
  compose.setContent {
   val density = LocalDensity.current
   CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
    FiadoTheme { Box(Modifier.width(280.dp)) { TotalCard(BigDecimal("1999999998")) } }
   }
  }
  val amount = compose.onNodeWithContentDescription("1.999.999.998 pesos").assertIsDisplayed()
  compose.onNodeWithText("Deslizá el importe para ver todas las cifras").assertIsDisplayed()
  val range = amount.fetchSemanticsNode().config[SemanticsProperties.HorizontalScrollAxisRange]
  assertTrue(range.maxValue() > 0f)
  amount.performTouchInput { swipeLeft() }
  compose.runOnIdle { assertTrue(range.value() > 0f) }
 }

 @Test fun everydayTotalFitsNarrowCardWithoutScrolling() {
  compose.setContent { FiadoTheme { Box(Modifier.width(280.dp)) { TotalCard(BigDecimal("11200")) } } }
  compose.onNodeWithContentDescription("11.200 pesos").assertIsDisplayed()
  compose.onNodeWithText("Deslizá el importe para ver todas las cifras").assertDoesNotExist()
 }
}

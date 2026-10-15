package ar.com.fiado.readings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

internal fun displayPesos(value: BigDecimal): String =
 DecimalFormat("#,##0", DecimalFormatSymbols(Locale.forLanguageTag("es-AR"))).format(value)

/** Fit in sp, never by overriding density. Extreme values remain readable via native scroll. */
@Composable
internal fun LargeAmount(value: BigDecimal) {
 val formatted = displayPesos(value)
 val text = "$ $formatted"
 val measurer = rememberTextMeasurer()
 val base = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-1).sp,
  fontFeatureSettings = "tnum")
 BoxWithConstraints(Modifier.fillMaxWidth()) {
  val sizes = listOf(48, 44, 40, 36, 32)
  val widthPx = constraints.maxWidth
  val size = sizes.firstOrNull { size ->
   measurer.measure(AnnotatedString(text), style = base.copy(fontSize = size.sp, lineHeight = (size + 10).sp),
    softWrap = false).size.width <= widthPx
  } ?: sizes.last()
  val style = base.copy(fontSize = size.sp, lineHeight = (size + 10).sp)
  val scrolls = measurer.measure(AnnotatedString(text), style = style, softWrap = false).size.width > widthPx
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
   Text(text, style = style, softWrap = false, color = MaterialTheme.colorScheme.onPrimaryContainer,
    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
     .clearAndSetSemantics { contentDescription = "$formatted pesos" })
   if (scrolls) Text("Deslizá el importe para ver todas las cifras", style = MaterialTheme.typography.bodySmall)
  }
 }
}

@Composable
internal fun TotalCard(total: BigDecimal?, label: String = "Total calculado") {
 Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
  Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
   Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
   if (total == null) Text("Pendiente de completar", style = MaterialTheme.typography.titleLarge)
   else LargeAmount(total)
   Text("Pesos argentinos · Detalle de Complicados", style = MaterialTheme.typography.bodySmall)
  }
 }
}

@Composable
internal fun SectionTitle(step: String, title: String) {
 Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
  Text(step, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
  Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
 }
}

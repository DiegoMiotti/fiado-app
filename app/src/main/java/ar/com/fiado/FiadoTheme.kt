package ar.com.fiado

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Native system typography keeps Android font scaling and familiar glyphs. */
@Composable
fun FiadoTheme(content: @Composable () -> Unit) {
 MaterialTheme(
  colorScheme = lightColorScheme(
   primary = Color(0xFF186048), onPrimary = Color.White,
   primaryContainer = Color(0xFFE0F1E8), onPrimaryContainer = Color(0xFF123E30),
   secondary = Color(0xFF486355), onSecondary = Color.White,
   secondaryContainer = Color(0xFFEBF3EE), onSecondaryContainer = Color(0xFF233F31),
   background = Color(0xFFF3F6F3), onBackground = Color(0xFF192D24),
   surface = Color.White, onSurface = Color(0xFF192D24),
   surfaceVariant = Color(0xFFE8EEE9), onSurfaceVariant = Color(0xFF475D50),
   outline = Color(0xFF718277), outlineVariant = Color(0xFFD3DED6),
   error = Color(0xFFA12622), errorContainer = Color(0xFFFFEDEA), onErrorContainer = Color(0xFF74211E)
  ),
  typography = Typography(
   displayMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 52.sp),
   headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
   headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
   titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
   titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
   bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 27.sp),
   bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
   bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
   labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp),
   labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
   labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp)
  ),
  shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp)),
  content = content
 )
}

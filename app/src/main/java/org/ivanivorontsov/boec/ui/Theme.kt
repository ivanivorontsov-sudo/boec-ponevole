package org.ivanivorontsov.boec.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.ivanivorontsov.boec.model.ThemeMode

val Ember = Color(0xFFFF6A3D)
val EmberDeep = Color(0xFFB8261B)
val Mint = Color(0xFF3DDC97)
val Amber = Color(0xFFFFC145)
val Danger = Color(0xFFFF5370)
val Sky = Color(0xFF5AB4FF)

private val Dark = darkColorScheme(
    primary = Ember, onPrimary = Color.White, primaryContainer = Color(0xFF3A1C12), onPrimaryContainer = Color(0xFFFFD7C8),
    secondary = Mint, onSecondary = Color(0xFF00281A), secondaryContainer = Color(0xFF113A2A), onSecondaryContainer = Color(0xFFB9F5D8),
    tertiary = Amber, onTertiary = Color(0xFF2A1C00), tertiaryContainer = Color(0xFF3A2C08), onTertiaryContainer = Color(0xFFFFE3A3),
    background = Color(0xFF0E1014), onBackground = Color(0xFFECEFF4),
    surface = Color(0xFF15181E), onSurface = Color(0xFFECEFF4),
    surfaceVariant = Color(0xFF1E222A), onSurfaceVariant = Color(0xFFA3AAB8),
    surfaceContainer = Color(0xFF181B22), surfaceContainerHigh = Color(0xFF1F232B), surfaceContainerHighest = Color(0xFF262B34),
    outline = Color(0xFF353B47), outlineVariant = Color(0xFF2A2F39), error = Danger, onError = Color.White,
)

private val Light = lightColorScheme(
    primary = Color(0xFFE04E22), onPrimary = Color.White, primaryContainer = Color(0xFFFFDCCF), onPrimaryContainer = Color(0xFF3A0F00),
    secondary = Color(0xFF14935E), onSecondary = Color.White, secondaryContainer = Color(0xFFC8F3DE), onSecondaryContainer = Color(0xFF00301D),
    tertiary = Color(0xFFB07800), onTertiary = Color.White, tertiaryContainer = Color(0xFFFFE6AE), onTertiaryContainer = Color(0xFF2A1C00),
    background = Color(0xFFF6F4F1), onBackground = Color(0xFF1A1C20),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1A1C20),
    surfaceVariant = Color(0xFFEFEBE6), onSurfaceVariant = Color(0xFF5E6470),
    surfaceContainer = Color(0xFFF9F7F4), surfaceContainerHigh = Color(0xFFF1EEEA), surfaceContainerHighest = Color(0xFFE9E5E0),
    outline = Color(0xFFD3CCC3), outlineVariant = Color(0xFFE6E0D8), error = Color(0xFFD32F4F), onError = Color.White,
)

private val base = Typography()
private val AppTypography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.3).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(lineHeight = 24.sp),
    bodyMedium = base.bodyMedium.copy(lineHeight = 21.sp),
)

val LocalDark = staticCompositionLocalOf { true }

@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
}

@Composable
fun BoecTheme(dark: Boolean, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(LocalDark provides dark) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, typography = AppTypography, content = content)
    }
}

val MonoDigits = TextStyle(fontWeight = FontWeight.Black, fontFeatureSettings = "tnum")

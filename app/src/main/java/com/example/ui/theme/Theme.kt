package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = FrostedIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = FrostedTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = FrostedAmber,
    onTertiary = Color.White,
    background = SlateBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = SlateSurface,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = GlassBorderColor,
    outlineVariant = Color(0x1AFFFFFF),
    error = FrostedRose,
    onError = Color.White,
    errorContainer = Color(0xFF881337),
    onErrorContainer = Color(0xFFFFD1D1)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF042F2E),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0x330F172A),
    error = Color(0xFFE11D48),
    onError = Color.White,
    errorContainer = Color(0xFFFFD1D1),
    onErrorContainer = Color(0xFF881337)
)

@Composable
fun YemenCurriculumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}


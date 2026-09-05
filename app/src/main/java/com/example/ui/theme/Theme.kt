package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = EmeraldSuccess,
    onSecondary = Color(0xFF003919),
    secondaryContainer = Color(0xFF005327),
    onSecondaryContainer = Color(0xFF66FFA3),
    tertiary = ShizukuViolet,
    onTertiary = Color(0xFF280068),
    tertiaryContainer = ShizukuVioletSurface,
    onTertiaryContainer = Color(0xFFEADBFF),
    background = TechDarkBackground,
    onBackground = TextPrimary,
    surface = TechDarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = TechDarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = TechDarkCardBorder,
    error = RedDanger,
    onError = Color(0xFF600004)
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

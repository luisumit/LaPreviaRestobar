package com.laprevia.restobar.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = AmberPrimary,
    onPrimary = NightBackground,
    primaryContainer = Color(0xFF5C4300),
    onPrimaryContainer = Color(0xFFFFDF9E),
    secondary = CoralSecondary,
    onSecondary = NightBackground,
    secondaryContainer = Color(0xFF932100),
    onSecondaryContainer = Color(0xFFFFDBD1),
    tertiary = Color(0xFF4FC3F7),
    onTertiary = Color(0xFF004D64),
    tertiaryContainer = Color(0xFF004D64),
    onTertiaryContainer = Color(0xFFB3E5FC),
    background = NightBackground,
    onBackground = SmokeWhite,
    surface = NightSurface,
    onSurface = SmokeWhite,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = SmokeWhiteSecondary,
    outline = Color(0xFF8E8E99),
    outlineVariant = Color(0xFF2A2A35),
    error = ErrorRed,
    onError = NightBackground,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD4),
)

@Composable
fun LaPreviaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = Typography,
        content = content
    )
}

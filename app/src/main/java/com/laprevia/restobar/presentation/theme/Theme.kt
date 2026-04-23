// presentation/theme/Theme.kt
package com.laprevia.restobar.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD32F2F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFCDD2),
    onPrimaryContainer = Color(0xFFB71C1C),
    secondary = Color(0xFFFFA000),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFFFFECB3),
    onSecondaryContainer = Color(0xFFFF8F00),
    tertiary = Color(0xFF2196F3),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBBDEFB),
    onTertiaryContainer = Color(0xFF1976D2),
    background = Color(0xFFFAFAFA),
    onBackground = Color(0xFF212121),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF212121),
    surfaceVariant = Color(0xFFE1E1E1),
    onSurfaceVariant = Color(0xFF757575),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFC4C4C4),
    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFF690005),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4A8),
    onPrimary = Color(0xFF680003),
    primaryContainer = Color(0xFF93000A),
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = Color(0xFFFFB4A8),
    onSecondary = Color(0xFF680003),
    secondaryContainer = Color(0xFF93000A),
    onSecondaryContainer = Color(0xFFFFDAD4),
    tertiary = Color(0xFFB2C9FF),
    onTertiary = Color(0xFF002C6E),
    tertiaryContainer = Color(0xFF02419B),
    onTertiaryContainer = Color(0xFFD8E2FF),
    background = Color(0xFF201A19),
    onBackground = Color(0xFFEDE0DD),
    surface = Color(0xFF201A19),
    onSurface = Color(0xFFEDE0DD),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BE),
    outline = Color(0xFFA08C89),
    outlineVariant = Color(0xFF534341),
    error = Color(0xFFFFB4A8),
    onError = Color(0xFF680003),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD4),
)

@Composable
fun LaPreviaRestoBarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography, // Usa la tipografía por defecto de Material3
        content = content
    )
}
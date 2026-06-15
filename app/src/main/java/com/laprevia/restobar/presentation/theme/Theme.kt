package com.laprevia.restobar.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ============================================================
// Esquema oscuro "Noche de Previa" - Material Design 3
// ============================================================

private val NochePreviaColorScheme = darkColorScheme(
    // -- Primario: Ambar Calido (#FFB300) --
    primary = AmberPrimary,
    onPrimary = NightBackground,
    primaryContainer = AmberContainer,
    onPrimaryContainer = OnAmberContainer,

    // -- Secundario: Naranja Coral (#FF6E40) --
    secondary = CoralSecondary,
    onSecondary = NightBackground,
    secondaryContainer = CoralContainer,
    onSecondaryContainer = OnCoralContainer,

    // -- Terciario: Cyan Claro --
    tertiary = CyanTertiary,
    onTertiary = CyanContainer,
    tertiaryContainer = CyanContainer,
    onTertiaryContainer = OnCyanContainer,

    // -- Fondo y Superficie --
    background = NightBackground,
    onBackground = SmokeWhite,
    surface = NightSurface,
    onSurface = SmokeWhite,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = SmokeWhiteSecondary,

    // -- Outline --
    outline = OutlineNight,
    outlineVariant = OutlineVariantNight,

    // -- Error --
    error = ErrorRed,
    onError = NightBackground,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
)

@Composable
fun LaPreviaRestoBarTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NochePreviaColorScheme,
        typography = LaPreviaTypography,
        content = content
    )
}

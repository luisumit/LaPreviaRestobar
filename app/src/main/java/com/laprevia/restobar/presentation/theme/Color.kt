package com.laprevia.restobar.presentation.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Paleta "Noche de Previa" - Material Design 3
// ============================================================

// -- Primario: Ambar Calido --
// Botones principales, iconos activos, elementos de marca
val AmberPrimary = Color(0xFFFFB300)
val AmberPrimaryVariant = Color(0xFFC68A00)
val AmberContainer = Color(0xFF5C4300)
val OnAmberContainer = Color(0xFFFFDF9E)

// -- Secundario: Naranja Coral --
// FAB, notificaciones, alertas, mesas ocupadas
val CoralSecondary = Color(0xFFFF6E40)
val CoralSecondaryVariant = Color(0xFFD84315)
val CoralContainer = Color(0xFF932100)
val OnCoralContainer = Color(0xFFFFDBD1)

// -- Terciario: Cyan Claro --
// Detalles, chips, badges secundarios
val CyanTertiary = Color(0xFF4FC3F7)
val CyanContainer = Color(0xFF004D64)
val OnCyanContainer = Color(0xFFB3E5FC)

// -- Fondo: Azul Noche Profundo --
val NightBackground = Color(0xFF12121A)

// -- Superficie: Gris Noche --
val NightSurface = Color(0xFF1E1E28)
val NightSurfaceVariant = Color(0xFF2A2A35)

// -- Texto --
val SmokeWhite = Color(0xFFF5F5F5)
val SmokeWhiteSecondary = Color(0xFFC5C5D0)
val SmokeWhiteDisabled = Color(0xFF8E8E99)

// -- Outline --
val OutlineNight = Color(0xFF8E8E99)
val OutlineVariantNight = Color(0xFF2A2A35)

// -- Error --
val ErrorRed = Color(0xFFFF5252)
val ErrorContainer = Color(0xFF93000A)
val OnErrorContainer = Color(0xFFFFDAD4)

// -- Estados semanticos --
val SuccessGreen = Color(0xFF66BB6A)
val WarningOrange = Color(0xFFFFB74D)
val InfoBlue = Color(0xFF42A5F5)

// -- Compatibilidad hacia atras (deprecados, eliminar gradualmente) --
@Deprecated("Usar AmberPrimary", ReplaceWith("AmberPrimary"))
val PrimaryRed = AmberPrimary

@Deprecated("Usar CoralSecondary", ReplaceWith("CoralSecondary"))
val SecondaryAmber = CoralSecondary

@Deprecated("Usar NightBackground", ReplaceWith("NightBackground"))
val BackgroundWhite = NightBackground

@Deprecated("Usar NightSurface", ReplaceWith("NightSurface"))
val SurfaceWhite = NightSurface

@Deprecated("Usar SmokeWhite", ReplaceWith("SmokeWhite"))
val OnPrimaryWhite = SmokeWhite

@Deprecated("Usar NightBackground", ReplaceWith("NightBackground"))
val OnSecondaryBlack = NightBackground

@Deprecated("Usar SmokeWhite", ReplaceWith("SmokeWhite"))
val TextPrimary = SmokeWhite

@Deprecated("Usar SmokeWhiteSecondary", ReplaceWith("SmokeWhiteSecondary"))
val TextSecondary = SmokeWhiteSecondary

@Deprecated("Usar SuccessGreen", ReplaceWith("SuccessGreen"))
val GreenSuccess = SuccessGreen

@Deprecated("Usar WarningOrange", ReplaceWith("WarningOrange"))
val OrangeWarning = WarningOrange

@Deprecated("Usar ErrorRed", ReplaceWith("ErrorRed"))
val RedError = ErrorRed

// -- Gradientes --
val BackgroundGradient = listOf(NightBackground, NightSurface)
val AccentGradient = listOf(AmberPrimary, CoralSecondary)

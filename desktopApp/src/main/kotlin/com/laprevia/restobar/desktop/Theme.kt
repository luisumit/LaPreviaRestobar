package com.laprevia.restobar.desktop

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sistema de diseno "Noche de Previa — elevada" del panel de escritorio.
 * Misma paleta ambar/coral/verde de la app Android sobre fondo nocturno,
 * con tipografia propia: Bebas Neue (titulos, numeros de mesa) + Manrope (cuerpo).
 */
object Lp {
    // Fondos
    val Bg = Color(0xFF0E0E15)          // fondo general del panel
    val BgDeep = Color(0xFF0B0B12)      // fondo del login
    val Sidebar = Color(0xFF14141C)     // barra lateral
    val Card = Color(0xFF1B1B25)        // tarjetas
    val Field = Color(0xFF0D0D15)       // campos de texto
    val FormPanel = Color(0xFF12121B)   // columna del formulario de login
    val Divider = Color(0xFF2A2A35)

    // Acentos
    val Amber = Color(0xFFFFB300)
    val AmberDeep = Color(0xFFFF9500)
    val Coral = Color(0xFFFF6E40)
    val Green = Color(0xFF66BB6A)
    val Warn = Color(0xFFFFB74D)
    val Red = Color(0xFFFF5252)

    // Textos
    val Text = Color(0xFFF5F5F5)
    val TextBright = Color(0xFFE7E7EE)
    val TextSoft = Color(0xFFB9B9C4)
    val TextDim = Color(0xFF8B8B98)
    val TextFaint = Color(0xFF797987)
    val TextMuted = Color(0xFF5C5C68)
    val OnAccent = Color(0xFF12121A)    // texto oscuro sobre botones ambar/coral

    // Bordes y radios
    val CardBorder = Color.White.copy(alpha = 0.06f)
    val FieldBorder = Color.White.copy(alpha = 0.10f)
}

val BebasFamily = FontFamily(Font(resource = "fonts/BebasNeue-Regular.ttf"))

val ManropeFamily = FontFamily(
    Font(resource = "fonts/Manrope-Regular.ttf", weight = FontWeight.Normal),
    Font(resource = "fonts/Manrope-SemiBold.ttf", weight = FontWeight.SemiBold),
    Font(resource = "fonts/Manrope-Bold.ttf", weight = FontWeight.Bold),
    Font(resource = "fonts/Manrope-ExtraBold.ttf", weight = FontWeight.ExtraBold)
)

/**
 * Cifras tabulares de Manrope: los montos y cronometros no "bailan" al refrescar.
 * Lleva fontFamily porque Text(style = X) REEMPLAZA el estilo del tema (no lo mezcla).
 */
val TabularNumbers = TextStyle(fontFamily = ManropeFamily, fontFeatureSettings = "tnum")

/** Aplica Manrope a toda la tipografia Material (botones, campos, chips...). */
private fun manropeTypography(): Typography {
    val base = Typography()
    fun TextStyle.mr() = copy(fontFamily = ManropeFamily)
    return Typography(
        displayLarge = base.displayLarge.mr(), displayMedium = base.displayMedium.mr(),
        displaySmall = base.displaySmall.mr(), headlineLarge = base.headlineLarge.mr(),
        headlineMedium = base.headlineMedium.mr(), headlineSmall = base.headlineSmall.mr(),
        titleLarge = base.titleLarge.mr(), titleMedium = base.titleMedium.mr(),
        titleSmall = base.titleSmall.mr(), bodyLarge = base.bodyLarge.mr(),
        bodyMedium = base.bodyMedium.mr(), bodySmall = base.bodySmall.mr(),
        labelLarge = base.labelLarge.mr(), labelMedium = base.labelMedium.mr(),
        labelSmall = base.labelSmall.mr()
    )
}

@Composable
fun LpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lp.Amber,
            onPrimary = Lp.OnAccent,
            secondary = Lp.Coral,
            background = Lp.Bg,
            surface = Lp.Card,
            onBackground = Lp.Text,
            onSurface = Lp.Text,
            outline = Lp.FieldBorder
        ),
        typography = manropeTypography()
    ) {
        // Scrollbars al tono del tema (las de fabrica son grises claras)
        CompositionLocalProvider(
            LocalScrollbarStyle provides ScrollbarStyle(
                minimalHeight = 24.dp,
                thickness = 6.dp,
                shape = RoundedCornerShape(3.dp),
                hoverDurationMillis = 240,
                unhoverColor = Color.White.copy(alpha = 0.08f),
                hoverColor = Color.White.copy(alpha = 0.22f)
            ),
            content = content
        )
    }
}

/**
 * Tarjeta con profundidad: fondo #1B1B25 + "sheen" superior sutil + borde
 * con luz que cae desde arriba (12% -> 3%). Si se pasa un color de estado,
 * el borde usa ese color en lugar del gradiente.
 */
fun Modifier.lpCard(radius: Dp = 16.dp, borderTint: Color? = null): Modifier {
    val shape = RoundedCornerShape(radius)
    val base = this.clip(shape)
        .background(Lp.Card)
        .background(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.045f),
                0.35f to Color.Transparent
            )
        )
    return if (borderTint != null) {
        base.border(1.dp, borderTint, shape)
    } else {
        base.border(
            1.dp,
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.03f))
            ),
            shape
        )
    }
}

/**
 * Hover de escritorio: cursor de mano + velo blanco animado al pasar el mouse.
 * Aplicar DESPUES del background del elemento (el velo se pinta encima).
 * Con enabled=false no hay ni velo ni cursor de mano (boton deshabilitado).
 */
@Composable
fun Modifier.lpHover(strength: Float = 0.05f, enabled: Boolean = true): Modifier {
    if (!enabled) return this
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val overlay by animateColorAsState(
        if (hovered) Color.White.copy(alpha = strength) else Color.Transparent,
        tween(120)
    )
    return this.hoverable(interaction)
        .pointerHoverIcon(PointerIcon.Hand)
        .background(overlay)
}

/** Colores de campo de texto del panel: fondo #0D0D15, foco ambar. */
@Composable
fun lpFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Lp.Field,
    unfocusedContainerColor = Lp.Field,
    focusedBorderColor = Lp.Amber.copy(alpha = 0.65f),
    unfocusedBorderColor = Lp.FieldBorder,
    focusedLabelColor = Lp.Amber,
    unfocusedLabelColor = Lp.TextDim,
    cursorColor = Lp.Amber,
    focusedTextColor = Lp.TextBright,
    unfocusedTextColor = Lp.TextBright
)

/** Pildora de estado (LIBRE, OCUPADA, EN PREPARACION...). */
@Composable
fun StatusPill(text: String, color: Color) {
    Box(
        Modifier.background(color.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text, color = color, fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 0.8.sp
        )
    }
}

/** Mini-KPI en chip (cifra Bebas + etiqueta), para las franjas de resumen. */
@Composable
fun StatChip(value: String, label: String, color: Color) {
    Column(
        Modifier.background(color.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(value, fontFamily = BebasFamily, fontSize = 22.sp, letterSpacing = 0.5.sp, color = color, style = TabularNumbers)
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp, color = Lp.TextDim)
    }
}

/** Titulo de seccion en Bebas Neue. */
@Composable
fun BebasTitle(text: String, size: Int = 36) {
    Text(
        text, fontFamily = BebasFamily, fontSize = size.sp,
        letterSpacing = 2.sp, color = Lp.Text
    )
}

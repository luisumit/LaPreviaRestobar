package com.laprevia.restobar.presentation.screens.chef.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.presentation.theme.GreenSuccess
import com.laprevia.restobar.presentation.theme.OrangeWarning
import com.laprevia.restobar.presentation.theme.RedError

@Composable
fun StatusChip(status: OrderStatus) {
    val (backgroundColor, textColor, text) = getStatusColorsAndText(status)

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(4.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun getStatusColorsAndText(status: OrderStatus): Triple<Color, Color, String> {
    return when (status) {
        OrderStatus.ENVIADO -> Triple(OrangeWarning, Color.White, "Enviado")
        OrderStatus.ACEPTADO -> Triple(RedError, Color.White, "Aceptado")
        OrderStatus.EN_PREPARACION -> Triple(OrangeWarning, Color.White, "En Prep.")
        OrderStatus.LISTO -> Triple(GreenSuccess, Color.White, "Listo")
        else -> Triple(Color.Gray, Color.White, "Desconocido")
    }
}

@Preview
@Composable
fun StatusChipPreview() {
    StatusChip(status = OrderStatus.ENVIADO)
}

@Preview
@Composable
fun StatusChipPreviewAceptado() {
    StatusChip(status = OrderStatus.ACEPTADO)
}

@Preview
@Composable
fun StatusChipPreviewPreparacion() {
    StatusChip(status = OrderStatus.EN_PREPARACION)
}

@Preview
@Composable
fun StatusChipPreviewListo() {
    StatusChip(status = OrderStatus.LISTO)
}

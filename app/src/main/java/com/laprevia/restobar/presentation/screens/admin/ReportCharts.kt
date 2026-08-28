package com.laprevia.restobar.presentation.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laprevia.restobar.presentation.theme.SuccessGreen
import com.laprevia.restobar.presentation.theme.WarningOrange
import com.laprevia.restobar.domain.model.DailySalesPoint
import com.laprevia.restobar.domain.model.ProductSalesPoint
import java.util.Locale

private fun chartMoney(value: Double): String = String.format(Locale.US, "%.2f", value)

@Composable
private fun ChartCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isTablet: Boolean,
    content: @Composable () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 18.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        subtitle,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            content()
        }
    }
}

/** Tendencia de ventas: barras verticales proporcionales al total de cada bucket. */
@Composable
fun SalesTrendChart(points: List<DailySalesPoint>, isTablet: Boolean) {
    ChartCard(
        title = "Tendencia de ventas",
        subtitle = "Total cobrado por periodo",
        icon = Icons.Default.BarChart,
        isTablet = isTablet
    ) {
        val maxValue = points.maxOfOrNull { it.total }?.takeIf { it > 0.0 } ?: 0.0
        if (points.isEmpty() || maxValue <= 0.0) {
            Text(
                "Aun no hay ventas en este periodo.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            return@ChartCard
        }

        // Con muchos buckets mostramos etiquetas intercaladas para que no se amontonen.
        val labelEvery = when {
            points.size <= 12 -> 1
            points.size <= 20 -> 2
            else -> 4
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isTablet) 170.dp else 140.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(if (points.size > 16) 2.dp else 4.dp)
        ) {
            points.forEach { point ->
                val fraction = if (point.total <= 0.0) 0f
                else (point.total / maxValue).toFloat().coerceIn(0.04f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (point.total >= maxValue) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        )
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (points.size > 16) 2.dp else 4.dp)
        ) {
            points.forEachIndexed { index, point ->
                Text(
                    text = if (index % labelEvery == 0) point.label else "",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Text(
            "Maximo: S/ ${chartMoney(maxValue)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

/** Desglose de metodos de pago: barra apilada horizontal + leyenda con montos y %. */
@Composable
fun PaymentBreakdownChart(cash: Double, yapePlin: Double, card: Double, isTablet: Boolean) {
    ChartCard(
        title = "Metodos de pago",
        subtitle = "Reparto del total cobrado",
        icon = Icons.Default.Payments,
        isTablet = isTablet
    ) {
        val total = cash + yapePlin + card
        val cashColor = SuccessGreen
        val yapeColor = WarningOrange
        val cardColor = MaterialTheme.colorScheme.tertiary

        if (total <= 0.0) {
            Text(
                "Aun no hay cobros en este periodo.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            return@ChartCard
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
                .clip(RoundedCornerShape(11.dp))
        ) {
            if (cash > 0.0) Box(modifier = Modifier.weight(cash.toFloat()).fillMaxHeight().background(cashColor))
            if (yapePlin > 0.0) Box(modifier = Modifier.weight(yapePlin.toFloat()).fillMaxHeight().background(yapeColor))
            if (card > 0.0) Box(modifier = Modifier.weight(card.toFloat()).fillMaxHeight().background(cardColor))
        }

        PaymentLegendRow("Efectivo", cash, total, cashColor)
        PaymentLegendRow("Yape/Plin", yapePlin, total, yapeColor)
        PaymentLegendRow("Tarjeta", card, total, cardColor)
    }
}

@Composable
private fun PaymentLegendRow(label: String, value: Double, total: Double, color: Color) {
    val percent = if (total > 0.0) (value / total * 100.0) else 0.0
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        Text(
            "S/ ${chartMoney(value)}  (${String.format(Locale.US, "%.0f", percent)}%)",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

/** Top productos: barras horizontales proporcionales a la cantidad vendida. */
@Composable
fun TopProductsChart(products: List<ProductSalesPoint>, isTablet: Boolean) {
    ChartCard(
        title = "Top productos",
        subtitle = "Los mas vendidos del periodo",
        icon = Icons.Default.Star,
        isTablet = isTablet
    ) {
        if (products.isEmpty()) {
            Text(
                "Aun no hay productos vendidos.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            return@ChartCard
        }
        val maxQty = products.maxOfOrNull { it.quantity }?.takeIf { it > 0 } ?: 1
        products.forEach { product ->
            val fraction = (product.quantity.toFloat() / maxQty).coerceIn(0.06f, 1f)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        product.name,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${product.quantity} u  ·  S/ ${chartMoney(product.total)}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(5.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        }
    }
}

/** Horas pico: barras verticales con el total cobrado por hora del dia. */
@Composable
fun PeakHoursChart(hours: List<DailySalesPoint>, isTablet: Boolean) {
    ChartCard(
        title = "Horas pico",
        subtitle = "Total cobrado por hora del dia",
        icon = Icons.Default.Schedule,
        isTablet = isTablet
    ) {
        val maxValue = hours.maxOfOrNull { it.total }?.takeIf { it > 0.0 } ?: 0.0
        if (hours.isEmpty() || maxValue <= 0.0) {
            Text(
                "Aun no hay ventas en este periodo.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            return@ChartCard
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isTablet) 170.dp else 140.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(if (hours.size > 12) 2.dp else 4.dp)
        ) {
            hours.forEach { point ->
                val fraction = if (point.total <= 0.0) 0f
                else (point.total / maxValue).toFloat().coerceIn(0.04f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(
                            if (point.total >= maxValue) WarningOrange
                            else WarningOrange.copy(alpha = 0.55f)
                        )
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(if (hours.size > 12) 2.dp else 4.dp)
        ) {
            hours.forEach { point ->
                Text(
                    text = point.label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Text(
            "Hora top: S/ ${chartMoney(maxValue)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

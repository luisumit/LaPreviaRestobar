// OrderCard.kt - VERSIÓN CORREGIDA
package com.laprevia.restobar.presentation.screens.waiter.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus

@Composable
fun OrderCard(
    order: Order,
    onMarkAsServed: () -> Unit, // 🔥 CORREGIDO: Hacerlo obligatorio, no opcional
    onStatusUpdate: (String) -> Unit = {} // Este puede mantenerse opcional
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mesa ${order.tableNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pedido #${order.id.takeLast(6)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    // Información del tiempo con más detalle
                    Text(
                        text = "🕒 ${getDetailedTimeAgo(order.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "S/. ${"%.2f".format(order.total)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OrderStatusChip(status = order.status)
                }
            }

            // Barra de progreso mejorada
            OrderProgressBar(status = order.status)

            // Estado y tiempo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = getStatusText(order.status),
                        style = MaterialTheme.typography.bodyMedium,
                        color = getStatusColor(order.status),
                        fontWeight = FontWeight.Medium
                    )

                    // Tiempo en estado actual
                    Text(
                        text = "En este estado: ${getTimeInCurrentState(order)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Botón para expandir/contraer
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Contraer" else "Expandir",
                        modifier = Modifier.rotate(if (expanded) 180f else 0f)
                    )
                }
            }

            // Notas del pedido
            order.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF9C4)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📝 Nota: $notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8D6E63),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Items del pedido (expandible)
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "📦 Items del pedido (${order.items.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    order.items.forEach { item ->
                        WaiterOrderItemRow(item = item)
                    }

                    // Resumen de items
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total items:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = order.items.sumOf { it.quantity }.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Acciones específicas para órdenes listas
            if (order.status == OrderStatus.LISTO) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    // Mensaje destacado
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Listo",
                                tint = Color(0xFF4CAF50)
                            )
                            Text(
                                text = "✅ PEDIDO LISTO - Entregar al cliente",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }

                    // Botón para marcar como servido
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            println("🖱️ OrderCard: Botón 'Marcar como entregado' presionado")
                            println("🖱️ Orden: ${order.id}, Mesa: ${order.tableNumber}")
                            onMarkAsServed() // 🔥 AHORA ESTÁ IMPLEMENTADO
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Marcar como servido",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Marcar como Entregado")
                    }
                }
            }

            // Indicador de última actualización
            Text(
                text = "🔄 Actualizado ${getDetailedTimeAgo(order.updatedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .align(Alignment.End)
            )
        }
    }
}

// Barra de progreso mejorada
@Composable
fun OrderProgressBar(status: OrderStatus) {
    val progress = when (status) {
        OrderStatus.ENVIADO -> 0.25f
        OrderStatus.ACEPTADO -> 0.5f
        OrderStatus.EN_PREPARACION -> 0.75f
        OrderStatus.LISTO -> 1.0f
        else -> 0f
    }

    val color = getStatusColor(status)

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = color.copy(alpha = 0.3f)
        )

        // Indicadores de etapas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(
                "Enviado" to (progress >= 0.25f),
                "Aceptado" to (progress >= 0.5f),
                "Preparando" to (progress >= 0.75f),
                "Listo" to (progress >= 1.0f)
            ).forEach { (stage, completed) ->
                Text(
                    text = stage,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (completed) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun WaiterOrderItemRow(item: com.laprevia.restobar.data.model.OrderItem) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // ✅ ACTUALIZADO: Usar productName en lugar de product.name
                Text(
                    text = "${item.quantity}x ${item.productName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                // Información adicional del producto
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "S/. ${"%.2f".format(item.unitPrice)} c/u",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "→ S/. ${"%.2f".format(item.subtotal)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Bold
                    )
                }

                // ✅ ACTUALIZADO: Usar productDescription en lugar de product.description
                if (item.productDescription.isNotBlank()) {
                    Text(
                        text = item.productDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2
                    )
                }

                // ✅ NUEVO: Mostrar categoría del producto
                if (item.productCategory.isNotBlank()) {
                    Text(
                        text = "Categoría: ${item.productCategory}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus) {
    val (backgroundColor, textColor) = getStatusColors(status)

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = getStatusShortText(status),
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// Función para calcular tiempo en estado actual
private fun getTimeInCurrentState(order: Order): String {
    val now = System.currentTimeMillis()
    val diff = now - order.updatedAt
    val minutes = diff / (1000 * 60)

    return when {
        minutes < 1 -> "ahora"
        minutes < 60 -> "hace ${minutes}m"
        else -> "hace ${minutes / 60}h"
    }
}

// Función de tiempo más detallada
private fun getDetailedTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)

    return when {
        days > 0 -> "hace ${days}d ${hours % 24}h"
        hours > 0 -> "hace ${hours}h ${minutes % 60}m"
        minutes > 0 -> "hace ${minutes}m"
        else -> "hace unos momentos"
    }
}

private fun getStatusColors(status: OrderStatus): Pair<Color, Color> {
    return when (status) {
        OrderStatus.ENVIADO -> Color(0xFF2196F3) to Color.White
        OrderStatus.ACEPTADO -> Color(0xFFFF9800) to Color.White
        OrderStatus.EN_PREPARACION -> Color(0xFFFF5722) to Color.White
        OrderStatus.LISTO -> Color(0xFF4CAF50) to Color.White
        else -> Color.Gray to Color.White
    }
}

private fun getStatusColor(status: OrderStatus): Color {
    return when (status) {
        OrderStatus.ENVIADO -> Color(0xFF2196F3)
        OrderStatus.ACEPTADO -> Color(0xFFFF9800)
        OrderStatus.EN_PREPARACION -> Color(0xFFFF5722)
        OrderStatus.LISTO -> Color(0xFF4CAF50)
        else -> Color.Gray
    }
}

private fun getStatusShortText(status: OrderStatus): String {
    return when (status) {
        OrderStatus.ENVIADO -> "ENVIADO"
        OrderStatus.ACEPTADO -> "ACEPTADO"
        OrderStatus.EN_PREPARACION -> "PREPARACIÓN"
        OrderStatus.LISTO -> "LISTO"
        else -> "DESCONOCIDO"
    }
}

private fun getStatusText(status: OrderStatus): String {
    return when (status) {
        OrderStatus.ENVIADO -> "Enviado a cocina"
        OrderStatus.ACEPTADO -> "Aceptado por cocina"
        OrderStatus.EN_PREPARACION -> "En preparación"
        OrderStatus.LISTO -> "Listo para servir"
        else -> "Estado desconocido"
    }
}
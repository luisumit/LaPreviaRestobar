// OrderCard.kt - VERSIÓN ACTUALIZADA CON BOTONES RESPONSIVOS
package com.laprevia.restobar.presentation.screens.waiter.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus

@Composable
fun OrderCard(
    order: Order,
    onMarkAsDelivered: () -> Unit = {},  // Para entregar comida (LISTO → ENTREGADO)
    onMarkAsServed: () -> Unit = {},     // Para liberar mesa (ENTREGADO → COMPLETED)
    onCancel: () -> Unit = {},           // Para cancelar pedido
    onStatusUpdate: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    // ✅ Detectar tamaño de pantalla
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // ✅ En tablet landscape, los botones pueden ser más grandes
    val buttonHeight = if (isTablet) 48.dp else 36.dp
    val buttonContentSpacing = if (isTablet) 8.dp else 4.dp
    val iconSize = if (isTablet) 20.dp else 18.dp

    // Diálogo de confirmación para cancelar
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar Pedido") },
            text = {
                Text("¿Estás seguro de que quieres cancelar este pedido?\n\nSe devolverá el stock y la mesa quedará libre.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCancel()
                        showCancelDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) {
                    Text("Sí, Cancelar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No, Volver")
                }
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 12.dp else 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 20.dp else 16.dp)
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
                        style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pedido #${order.id.takeLast(6)}",
                        style = if (isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "🕒 ${getDetailedTimeAgo(order.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "S/. ${"%.2f".format(order.total)}",
                        style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OrderStatusChip(status = order.status)
                }
            }

            // Barra de progreso
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
                        style = if (isTablet) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        color = getStatusColor(order.status),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "En este estado: ${getTimeInCurrentState(order)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(if (isTablet) 32.dp else 24.dp)
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
                        modifier = Modifier.padding(if (isTablet) 12.dp else 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📝 Nota: $notes",
                            style = if (isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
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
                        .padding(top = if (isTablet) 16.dp else 12.dp)
                ) {
                    Text(
                        text = "📦 Items del pedido (${order.items.size})",
                        style = if (isTablet) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    order.items.forEach { item ->
                        WaiterOrderItemRow(item = item, isTablet = isTablet)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total items:",
                            style = if (isTablet) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = order.items.sumOf { it.quantity }.toString(),
                            style = if (isTablet) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ✅ BOTONES RESPONSIVOS SEGÚN EL ESTADO DE LA ORDEN
            when (order.status) {
                OrderStatus.LISTO -> {
                    Spacer(modifier = Modifier.height(if (isTablet) 12.dp else 8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(if (isTablet) 16.dp else 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Listo",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                            )
                            Text(
                                text = "✅ PEDIDO LISTO - Entregar al cliente",
                                style = if (isTablet) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(if (isTablet) 12.dp else 8.dp))

                    // ✅ Botones responsivos - En tablet se ven mejor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 8.dp)
                    ) {
                        // Botón CANCELAR
                        Button(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.weight(1f).height(buttonHeight),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            contentPadding = PaddingValues(horizontal = if (isTablet) 16.dp else 8.dp, vertical = if (isTablet) 8.dp else 4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(buttonContentSpacing),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Cancelar",
                                    modifier = Modifier.size(iconSize)
                                )
                                Text(
                                    if (isTablet) "❌ CANCELAR PEDIDO" else "CANCELAR",
                                    color = Color.White,
                                    fontSize = if (isTablet) MaterialTheme.typography.bodyMedium.fontSize else MaterialTheme.typography.labelLarge.fontSize
                                )
                            }
                        }
                        // Botón ENTREGAR
                        Button(
                            onClick = {
                                println("🍽️ OrderCard: Entregando comida - Orden ${order.id}")
                                onMarkAsDelivered()
                            },
                            modifier = Modifier.weight(1f).height(buttonHeight),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                            contentPadding = PaddingValues(horizontal = if (isTablet) 16.dp else 8.dp, vertical = if (isTablet) 8.dp else 4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(buttonContentSpacing),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Entregar comida",
                                    modifier = Modifier.size(iconSize)
                                )
                                Text(
                                    if (isTablet) "🍽️ ENTREGAR COMIDA" else "ENTREGAR",
                                    color = Color.White,
                                    fontSize = if (isTablet) MaterialTheme.typography.bodyMedium.fontSize else MaterialTheme.typography.labelLarge.fontSize
                                )
                            }
                        }
                    }
                }
                OrderStatus.ENTREGADO -> {
                    Spacer(modifier = Modifier.height(if (isTablet) 12.dp else 8.dp))

                    // ✅ Botones responsivos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 8.dp)
                    ) {
                        // Botón CANCELAR
                        Button(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.weight(1f).height(buttonHeight),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            contentPadding = PaddingValues(horizontal = if (isTablet) 16.dp else 8.dp, vertical = if (isTablet) 8.dp else 4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(buttonContentSpacing),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "Cancelar",
                                    modifier = Modifier.size(iconSize)
                                )
                                Text(
                                    if (isTablet) "❌ CANCELAR" else "CANCELAR",
                                    color = Color.White,
                                    fontSize = if (isTablet) MaterialTheme.typography.bodyMedium.fontSize else MaterialTheme.typography.labelLarge.fontSize
                                )
                            }
                        }
                        // Botón LIBERAR MESA
                        Button(
                            onClick = {
                                println("🧹 OrderCard: Liberando mesa - Orden ${order.id}")
                                onMarkAsServed()
                            },
                            modifier = Modifier.weight(1f).height(buttonHeight),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            contentPadding = PaddingValues(horizontal = if (isTablet) 16.dp else 8.dp, vertical = if (isTablet) 8.dp else 4.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(buttonContentSpacing),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Liberar mesa",
                                    modifier = Modifier.size(iconSize)
                                )
                                Text(
                                    if (isTablet) "🧹 LIBERAR MESA" else "LIBERAR",
                                    color = Color.White,
                                    fontSize = if (isTablet) MaterialTheme.typography.bodyMedium.fontSize else MaterialTheme.typography.labelLarge.fontSize
                                )
                            }
                        }
                    }
                }
                else -> {
                    // Para estados ENVIADO, ACEPTADO, EN_PREPARACION - solo botón cancelar
                    Spacer(modifier = Modifier.height(if (isTablet) 12.dp else 8.dp))
                    Button(
                        onClick = { showCancelDialog = true },
                        modifier = Modifier.fillMaxWidth().height(buttonHeight),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                        contentPadding = PaddingValues(horizontal = if (isTablet) 16.dp else 8.dp, vertical = if (isTablet) 8.dp else 4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(buttonContentSpacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancelar pedido",
                                modifier = Modifier.size(iconSize)
                            )
                            Text(
                                if (isTablet) "❌ CANCELAR PEDIDO" else "CANCELAR",
                                color = Color.White,
                                fontSize = if (isTablet) MaterialTheme.typography.bodyMedium.fontSize else MaterialTheme.typography.labelLarge.fontSize
                            )
                        }
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

@Composable
fun OrderProgressBar(status: OrderStatus) {
    val progress = when (status) {
        OrderStatus.ENVIADO -> 0.2f
        OrderStatus.ACEPTADO -> 0.4f
        OrderStatus.EN_PREPARACION -> 0.6f
        OrderStatus.LISTO -> 0.8f
        OrderStatus.ENTREGADO -> 0.9f
        OrderStatus.COMPLETED -> 1.0f
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(
                "Enviado" to (progress >= 0.2f),
                "Aceptado" to (progress >= 0.4f),
                "Preparando" to (progress >= 0.6f),
                "Listo" to (progress >= 0.8f),
                "Entregado" to (progress >= 0.9f)
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
fun WaiterOrderItemRow(item: com.laprevia.restobar.data.model.OrderItem, isTablet: Boolean = false) {
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
                .padding(if (isTablet) 16.dp else 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.quantity}x ${item.productName}",
                    style = if (isTablet) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

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

                if (item.productDescription.isNotBlank()) {
                    Text(
                        text = item.productDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2
                    )
                }

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
        OrderStatus.ENTREGADO -> Color(0xFF9C27B0) to Color.White
        OrderStatus.COMPLETED -> Color(0xFF9E9E9E) to Color.White
        OrderStatus.CANCELLED -> Color(0xFFF44336) to Color.White
        else -> Color.Gray to Color.White
    }
}

private fun getStatusColor(status: OrderStatus): Color {
    return when (status) {
        OrderStatus.ENVIADO -> Color(0xFF2196F3)
        OrderStatus.ACEPTADO -> Color(0xFFFF9800)
        OrderStatus.EN_PREPARACION -> Color(0xFFFF5722)
        OrderStatus.LISTO -> Color(0xFF4CAF50)
        OrderStatus.ENTREGADO -> Color(0xFF9C27B0)
        OrderStatus.COMPLETED -> Color(0xFF9E9E9E)
        OrderStatus.CANCELLED -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

private fun getStatusShortText(status: OrderStatus): String {
    return when (status) {
        OrderStatus.ENVIADO -> "ENVIADO"
        OrderStatus.ACEPTADO -> "ACEPTADO"
        OrderStatus.EN_PREPARACION -> "PREPARACIÓN"
        OrderStatus.LISTO -> "LISTO"
        OrderStatus.ENTREGADO -> "ENTREGADO"
        OrderStatus.COMPLETED -> "COMPLETADO"
        OrderStatus.CANCELLED -> "CANCELADO"
        else -> "DESCONOCIDO"
    }
}

private fun getStatusText(status: OrderStatus): String {
    return when (status) {
        OrderStatus.ENVIADO -> "Enviado a cocina"
        OrderStatus.ACEPTADO -> "Aceptado por cocina"
        OrderStatus.EN_PREPARACION -> "En preparación"
        OrderStatus.LISTO -> "Listo para servir"
        OrderStatus.ENTREGADO -> "Comida entregada"
        OrderStatus.COMPLETED -> "Completado"
        OrderStatus.CANCELLED -> "Cancelado"
        else -> "Estado desconocido"
    }
}
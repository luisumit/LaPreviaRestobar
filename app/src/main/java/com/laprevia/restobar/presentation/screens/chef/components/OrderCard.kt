package com.laprevia.restobar.presentation.screens.chef.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
    onUpdateStatus: (OrderStatus) -> Unit,
    quickActions: List<QuickAction> = emptyList(),
    showCompletionOption: Boolean = false,
    onMarkAsCompleted: (() -> Unit)? = null
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
                        text = "Orden #${order.id.takeLast(6)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "👤 ${order.waiterName ?: order.waiterId ?: "Mesero"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "🕒 ${getTimeAgo(order.createdAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                StatusChip(status = order.status)
            }

            // Información rápida
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${order.items.size} items • S/. ${"%.2f".format(order.total)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

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

            // Notas especiales del pedido
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
                            text = "📝 $notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8D6E63),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Items del pedido
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    if (order.items.isNotEmpty()) {
                        Text(
                            text = "📦 Items del pedido (${order.items.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        order.items.forEach { item ->
                            OrderItemRow(item = item)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total:",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "S/. ${"%.2f".format(order.total)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = "⚠️ No hay items en esta orden",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            OrderActionButtons(
                status = order.status,
                onUpdateStatus = onUpdateStatus,
                quickActions = quickActions,
                showCompletionOption = showCompletionOption,
                onMarkAsCompleted = onMarkAsCompleted
            )
        }
    }
}

@Composable
fun OrderItemRow(item: com.laprevia.restobar.data.model.OrderItem) {
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
                Text(
                    text = "${item.quantity}x ${item.productName ?: "Producto"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                if (!item.productDescription.isNullOrBlank()) {
                    Text(
                        text = item.productDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!item.productCategory.isNullOrBlank()) {
                        Text(
                            text = "📁 ${item.productCategory}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }

                    if (item.trackInventory) {
                        Text(
                            text = "📦 Control Stock",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFF9800)
                        )
                    }

                    Text(
                        text = "S/. ${"%.2f".format(item.unitPrice)} c/u",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "S/. ${"%.2f".format(item.subtotal)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "ID: ${(item.productId ?: "N/A").take(8)}...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun OrderActionButtons(
    status: OrderStatus,
    onUpdateStatus: (OrderStatus) -> Unit,
    quickActions: List<QuickAction> = emptyList(),
    showCompletionOption: Boolean = false,
    onMarkAsCompleted: (() -> Unit)? = null
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (quickActions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickActions.forEach { action ->
                    Button(
                        onClick = { onUpdateStatus(action.status) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = action.color
                        )
                    ) {
                        Text(action.label)
                    }
                }
            }
        } else {
            when (status) {
                OrderStatus.ENVIADO -> {
                    Button(
                        onClick = { onUpdateStatus(OrderStatus.ACEPTADO) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        )
                    ) {
                        Text("✅ Aceptar Pedido")
                    }
                }
                OrderStatus.ACEPTADO -> {
                    Button(
                        onClick = { onUpdateStatus(OrderStatus.EN_PREPARACION) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF5722)
                        )
                    ) {
                        Text("👨‍🍳 Comenzar Preparación")
                    }
                }
                OrderStatus.EN_PREPARACION -> {
                    Button(
                        onClick = { onUpdateStatus(OrderStatus.LISTO) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text("🎉 Marcar como Listo")
                    }
                }
                OrderStatus.LISTO -> {
                    Column {
                        Text(
                            text = "✅ PEDIDO LISTO PARA SERVIR",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )

                        if (showCompletionOption && onMarkAsCompleted != null) {
                            OutlinedButton(
                                onClick = onMarkAsCompleted,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            ) {
                                Text("🗑️ Marcar como Completado")
                            }
                        }
                    }
                }
                OrderStatus.ENTREGADO -> {
                    Text(
                        text = "🍽️ COMIDA ENTREGADA - Esperando liberar mesa",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF9C27B0),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
                OrderStatus.COMPLETED -> {
                    Text(
                        text = "✅ PEDIDO COMPLETADO",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF9E9E9E),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    )
                }
                else -> {
                    Text(
                        text = "Estado: ${status.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)

    return when {
        minutes < 1 -> "Ahora"
        minutes < 60 -> "Hace ${minutes}m"
        hours < 24 -> "Hace ${hours}h"
        else -> "Hace ${hours / 24}d"
    }
}

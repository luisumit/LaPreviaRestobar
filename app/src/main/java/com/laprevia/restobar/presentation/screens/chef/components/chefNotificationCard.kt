package com.laprevia.restobar.presentation.screens.chef.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laprevia.restobar.presentation.viewmodel.ChefViewModel

@Composable
fun ChefNotificationCard(
    notification: ChefViewModel.ChefNotification,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (notification.type) {
        ChefViewModel.ChefNotificationType.NEW_ORDER -> Color(0xFFFFEB3B).copy(alpha = 0.1f)
        ChefViewModel.ChefNotificationType.ORDER_ACCEPTED -> Color(0xFF2196F3).copy(alpha = 0.1f)
        ChefViewModel.ChefNotificationType.ORDER_IN_PREPARATION -> Color(0xFFFF9800).copy(alpha = 0.1f)
        ChefViewModel.ChefNotificationType.ORDER_READY -> Color(0xFF4CAF50).copy(alpha = 0.1f)
        ChefViewModel.ChefNotificationType.ORDER_DELIVERED -> Color(0xFFFF9800).copy(alpha = 0.15f)  // ✅ NUEVO
        ChefViewModel.ChefNotificationType.ORDER_CANCELLED -> Color(0xFFF44336).copy(alpha = 0.1f)
        ChefViewModel.ChefNotificationType.INVENTORY_UPDATED -> Color(0xFF9C27B0).copy(alpha = 0.1f)
    }

    val icon = when (notification.type) {
        ChefViewModel.ChefNotificationType.NEW_ORDER -> Icons.Default.NewReleases
        ChefViewModel.ChefNotificationType.ORDER_ACCEPTED -> Icons.Default.Restaurant
        ChefViewModel.ChefNotificationType.ORDER_IN_PREPARATION -> Icons.Default.LocalBar
        ChefViewModel.ChefNotificationType.ORDER_READY -> Icons.Default.CheckCircle
        ChefViewModel.ChefNotificationType.ORDER_DELIVERED -> Icons.Default.Restaurant  // ✅ NUEVO (o Icons.Default.LocalDining)
        ChefViewModel.ChefNotificationType.ORDER_CANCELLED -> Icons.Default.Close
        ChefViewModel.ChefNotificationType.INVENTORY_UPDATED -> Icons.Default.Inventory
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (notification.itemsCount > 0) {
                    Text(
                        text = "${notification.itemsCount} items • S/. ${"%.2f".format(notification.totalAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = "Hace ${getTimeAgo(notification.timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar notificación",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun getTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        hours > 0 -> "$hours h"
        minutes > 0 -> "$minutes min"
        else -> "ahora"
    }
}
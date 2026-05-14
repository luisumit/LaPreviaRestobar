package com.laprevia.restobar.presentation.screens.waiter.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laprevia.restobar.presentation.viewmodel.WaiterViewModel

@Composable
fun NotificationCard(
    notification: WaiterViewModel.Notification,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (notification.type) {
        WaiterViewModel.NotificationType.ORDER_READY -> Color(0xFF4CAF50).copy(alpha = 0.1f)
        WaiterViewModel.NotificationType.ORDER_ACCEPTED -> Color(0xFF2196F3).copy(alpha = 0.1f)
        WaiterViewModel.NotificationType.ORDER_IN_PREPARATION -> Color(0xFFFF9800).copy(alpha = 0.1f)
        WaiterViewModel.NotificationType.ORDER_SENT -> Color(0xFF9C27B0).copy(alpha = 0.1f)
        WaiterViewModel.NotificationType.ORDER_CANCELLED -> Color(0xFFF44336).copy(alpha = 0.1f)
        WaiterViewModel.NotificationType.ORDER_DELIVERED -> Color(0xFF9C27B0).copy(alpha = 0.1f)  // ✅ NUEVO
    }

    val icon = when (notification.type) {
        WaiterViewModel.NotificationType.ORDER_READY -> Icons.Default.CheckCircle
        WaiterViewModel.NotificationType.ORDER_ACCEPTED -> Icons.Default.Restaurant
        WaiterViewModel.NotificationType.ORDER_IN_PREPARATION -> Icons.Default.LocalBar
        WaiterViewModel.NotificationType.ORDER_SENT -> Icons.Default.Schedule
        WaiterViewModel.NotificationType.ORDER_CANCELLED -> Icons.Default.Close
        WaiterViewModel.NotificationType.ORDER_DELIVERED -> Icons.Default.CheckCircle  // ✅ NUEVO
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

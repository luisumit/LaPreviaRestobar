// OrdersScreen.kt - VERSIÓN CORREGIDA
package com.laprevia.restobar.presentation.screens.chef

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.presentation.screens.chef.components.OrderCard
import com.laprevia.restobar.presentation.screens.chef.components.QuickAction
import com.laprevia.restobar.presentation.viewmodel.ChefViewModel
import kotlinx.coroutines.delay

@Composable
fun OrdersScreen(
    viewModel: ChefViewModel = hiltViewModel()
) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()
    val isInternetAvailable by viewModel.isInternetAvailable.collectAsState()
    val connectionMessage by viewModel.connectionMessage.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    // Auto-clear mensaje de conexión después de 3 segundos
    LaunchedEffect(connectionMessage) {
        if (connectionMessage != null) {
            delay(3000)
            viewModel.clearConnectionMessage()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshOrders()
    }

    LaunchedEffect(notifications) {
        if (notifications.isNotEmpty()) {
            println("📢 Notificaciones del chef: ${notifications.size}")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Banner de estado de conexión actualizado
        ConnectionStatusBannerOrders(
            isInternetAvailable = isInternetAvailable,
            isFirebaseConnected = isFirebaseConnected,
            isLoading = isLoading,
            connectionMessage = connectionMessage,
            onManualSync = { viewModel.manualSync() }  // ✅ Pasar la función
        )

        // Agrupar órdenes por estado
        val ordersByStatus = orders.groupBy { it.status }
        val sentOrders = ordersByStatus[OrderStatus.ENVIADO] ?: emptyList()
        val acceptedOrders = ordersByStatus[OrderStatus.ACEPTADO] ?: emptyList()
        val inPreparationOrders = ordersByStatus[OrderStatus.EN_PREPARACION] ?: emptyList()
        val readyOrders = ordersByStatus[OrderStatus.LISTO] ?: emptyList()

        if (isLoading && orders.isEmpty()) {
            LoadingState(isInternetAvailable = isInternetAvailable)
        } else if (orders.isEmpty()) {
            EmptyChefOrdersState(
                isFirebaseConnected = isFirebaseConnected,
                isInternetAvailable = isInternetAvailable,
                onManualSync = { viewModel.manualSync() }  // ✅ Pasar la función
            )
        } else {
            OrdersList(
                sentOrders = sentOrders,
                acceptedOrders = acceptedOrders,
                inPreparationOrders = inPreparationOrders,
                readyOrders = readyOrders,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun ConnectionStatusBannerOrders(
    isInternetAvailable: Boolean,
    isFirebaseConnected: Boolean,
    isLoading: Boolean,
    connectionMessage: String?,
    onManualSync: () -> Unit  // ✅ Nuevo parámetro
) {
    if (isLoading) return

    // Mostrar mensaje temporal si existe
    if (connectionMessage != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    connectionMessage.contains("SIN INTERNET") -> Color(0xFFF44336).copy(alpha = 0.9f)
                    connectionMessage.contains("guardado localmente") -> Color(0xFFFF9800).copy(alpha = 0.9f)
                    else -> Color(0xFF4CAF50).copy(alpha = 0.9f)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        connectionMessage.contains("SIN INTERNET") -> Icons.Default.WifiOff
                        connectionMessage.contains("guardado localmente") -> Icons.Default.Warning
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = connectionMessage,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Banner de estado permanente
    val statusColor = when {
        !isInternetAvailable -> Color(0xFFF44336)
        !isFirebaseConnected -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    val statusText = when {
        !isInternetAvailable -> "🔴 SIN INTERNET - Modo offline"
        !isFirebaseConnected -> "🟡 Reconectando con meseros..."
        else -> "🟢 Conectado a meseros"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when {
                        !isInternetAvailable -> Icons.Default.WifiOff
                        !isFirebaseConnected -> Icons.Default.Sync
                        else -> Icons.Default.Wifi
                    },
                    contentDescription = "Estado de conexión",
                    tint = statusColor
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!isFirebaseConnected && isInternetAvailable) {
                TextButton(
                    onClick = onManualSync,
                    colors = ButtonDefaults.textButtonColors(contentColor = statusColor)
                ) {
                    Text("Reconectar", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                }
            }
        }
    }
}

@Composable
fun OrdersList(
    sentOrders: List<Order>,
    acceptedOrders: List<Order>,
    inPreparationOrders: List<Order>,
    readyOrders: List<Order>,
    viewModel: ChefViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OrdersSummary(
                newCount = sentOrders.size,
                acceptedCount = acceptedOrders.size,
                inProgressCount = inPreparationOrders.size,
                readyCount = readyOrders.size
            )
        }

        // Órdenes ENVIADAS (Nuevas) - PRIORIDAD ALTA
        if (sentOrders.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "🆕 Nuevas Órdenes",
                    count = sentOrders.size,
                    color = Color(0xFF2196F3),
                    description = "Órdenes recién enviadas por meseros"
                )
            }
            items(sentOrders) { order ->
                OrderCard(
                    order = order,
                    onUpdateStatus = { status ->
                        when (status) {
                            OrderStatus.ACEPTADO -> viewModel.acceptOrder(order.id)
                            OrderStatus.EN_PREPARACION -> viewModel.startOrderPreparation(order.id)
                            OrderStatus.LISTO -> viewModel.markOrderAsReady(order.id)
                            else -> viewModel.updateOrderStatus(order.id, status)
                        }
                    },
                    quickActions = listOf(
                        QuickAction(
                            label = "Aceptar",
                            status = OrderStatus.ACEPTADO,
                            color = Color(0xFFFF9800)
                        ),
                        QuickAction(
                            label = "Preparar",
                            status = OrderStatus.EN_PREPARACION,
                            color = Color(0xFFFF5722)
                        )
                    )
                )
            }
        }

        // Órdenes ACEPTADAS
        if (acceptedOrders.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "✅ Aceptadas",
                    count = acceptedOrders.size,
                    color = Color(0xFFFF9800),
                    description = "Órdenes aceptadas pendientes de preparación"
                )
            }
            items(acceptedOrders) { order ->
                OrderCard(
                    order = order,
                    onUpdateStatus = { status ->
                        when (status) {
                            OrderStatus.EN_PREPARACION -> viewModel.startOrderPreparation(order.id)
                            OrderStatus.LISTO -> viewModel.markOrderAsReady(order.id)
                            else -> viewModel.updateOrderStatus(order.id, status)
                        }
                    },
                    quickActions = listOf(
                        QuickAction(
                            label = "Comenzar",
                            status = OrderStatus.EN_PREPARACION,
                            color = Color(0xFFFF5722)
                        )
                    )
                )
            }
        }

        // Órdenes EN PREPARACIÓN
        if (inPreparationOrders.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "👨‍🍳 En Preparación",
                    count = inPreparationOrders.size,
                    color = Color(0xFFFF5722),
                    description = "Órdenes en proceso de preparación"
                )
            }
            items(inPreparationOrders) { order ->
                OrderCard(
                    order = order,
                    onUpdateStatus = { status ->
                        when (status) {
                            OrderStatus.LISTO -> viewModel.markOrderAsReady(order.id)
                            else -> viewModel.updateOrderStatus(order.id, status)
                        }
                    },
                    quickActions = listOf(
                        QuickAction(
                            label = "Listo",
                            status = OrderStatus.LISTO,
                            color = Color(0xFF4CAF50)
                        )
                    )
                )
            }
        }

        // Órdenes LISTAS
        if (readyOrders.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "🎉 Listas para Servir",
                    count = readyOrders.size,
                    color = Color(0xFF4CAF50),
                    description = "Órdenes listas para que los meseros sirvan"
                )
            }
            items(readyOrders) { order ->
                OrderCard(
                    order = order,
                    onUpdateStatus = { status ->
                        viewModel.updateOrderStatus(order.id, status)
                    },
                    showCompletionOption = true,
                    onMarkAsCompleted = {
                        viewModel.updateOrderStatus(order.id, OrderStatus.COMPLETED)
                    }
                )
            }
        }
    }
}

@Composable
fun OrdersSummary(
    newCount: Int,
    acceptedCount: Int,
    inProgressCount: Int,
    readyCount: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "📊 Resumen de Pedidos",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SummaryItem(count = newCount, label = "Nuevos", color = Color(0xFF2196F3))
                SummaryItem(count = acceptedCount, label = "Aceptados", color = Color(0xFFFF9800))
                SummaryItem(count = inProgressCount, label = "En Prep.", color = Color(0xFFFF5722))
                SummaryItem(count = readyCount, label = "Listos", color = Color(0xFF4CAF50))
            }
        }
    }
}

@Composable
fun SummaryItem(count: Int, label: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = color,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text = count.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    color: Color,
    description: String = ""
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )

                Surface(
                    color = color,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = count.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun LoadingState(isInternetAvailable: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = if (!isInternetAvailable) "Sin conexión - Modo offline" else "Conectando con cocina...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun EmptyChefOrdersState(
    isFirebaseConnected: Boolean,
    isInternetAvailable: Boolean,
    onManualSync: () -> Unit  // ✅ Nuevo parámetro
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = when {
                    !isInternetAvailable -> Icons.Default.WifiOff
                    isFirebaseConnected -> Icons.Default.Restaurant
                    else -> Icons.Default.Sync
                },
                contentDescription = "Sin pedidos",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = when {
                    !isInternetAvailable -> "Sin conexión a internet"
                    isFirebaseConnected -> "No hay pedidos pendientes"
                    else -> "Conectando con el sistema"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = when {
                    !isInternetAvailable -> "Los pedidos se sincronizarán cuando vuelva internet"
                    isFirebaseConnected -> "Los pedidos enviados por los meseros aparecerán aquí"
                    else -> "Esperando conexión con Firebase"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )

            if (!isInternetAvailable) {
                Button(
                    onClick = onManualSync,  // ✅ Usar el parámetro recibido
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
                ) {
                    Text("Reintentar conexión")
                }
            }
        }
    }
}
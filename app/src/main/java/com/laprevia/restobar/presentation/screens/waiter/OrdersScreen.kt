package com.laprevia.restobar.presentation.screens.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.presentation.screens.waiter.components.OrderCard
import com.laprevia.restobar.presentation.viewmodel.WaiterViewModel
import kotlinx.coroutines.delay

@Composable
fun OrdersScreen(
    navController: NavController,
    viewModel: WaiterViewModel = hiltViewModel()
) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()
    val isInternetAvailable by viewModel.isInternetAvailable.collectAsState()
    val connectionMessage by viewModel.connectionMessage.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    val currentConnectionMessage = connectionMessage

    LaunchedEffect(currentConnectionMessage) {
        if (currentConnectionMessage != null) {
            delay(3000)
            viewModel.clearConnectionMessage()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    val connectionStatusText = when {
        !isInternetAvailable -> "🔴 SIN INTERNET - Modo offline"
        isFirebaseConnected -> "🟢 Conectado a cocina"
        else -> "🟡 Reconectando..."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0f3460))
    ) {
        FirebaseConnectionBanner(
            isConnected = isFirebaseConnected,
            isInternetAvailable = isInternetAvailable,
            connectionStatus = connectionStatusText,
            isLoading = isLoading,
            onSyncClick = { viewModel.syncWithFirebase() }
        )

        if (currentConnectionMessage != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (currentConnectionMessage.contains("SIN INTERNET"))
                        Color(0xFFFF9800).copy(alpha = 0.9f)
                    else
                        Color(0xFF4CAF50).copy(alpha = 0.9f)
                )
            ) {
                Text(
                    text = currentConnectionMessage,
                    color = Color.White,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        OrdersSummary(
            totalOrders = orders.size,
            pendingOrders = orders.count { it.status == OrderStatus.ENVIADO },
            inProgressOrders = orders.count { it.status == OrderStatus.EN_PREPARACION },
            readyOrders = orders.count { it.status == OrderStatus.LISTO },
            isFirebaseConnected = isFirebaseConnected,
            isInternetAvailable = isInternetAvailable,
            modifier = Modifier.padding(16.dp)
        )

        if (isLoading && orders.isEmpty()) {
            LoadingState(isInternetAvailable = isInternetAvailable)
        } else if (orders.isEmpty()) {
            EmptyOrdersState(
                isFirebaseConnected = isFirebaseConnected,
                isInternetAvailable = isInternetAvailable,
                onRetry = { viewModel.syncWithFirebase() }
            )
        } else {
            if (!isInternetAvailable) {
                Text(
                    text = "📱 Modo offline - Mostrando órdenes guardadas localmente",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            OrdersListContent(
                orders = orders,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun FirebaseConnectionBanner(
    isConnected: Boolean,
    isInternetAvailable: Boolean,
    connectionStatus: String,
    isLoading: Boolean,
    onSyncClick: () -> Unit
) {
    if (isLoading) return

    val backgroundColor = when {
        !isInternetAvailable -> Color(0xFFF44336).copy(alpha = 0.15f)
        !isConnected -> Color(0xFFFF9800).copy(alpha = 0.15f)
        else -> Color(0xFF4CAF50).copy(alpha = 0.15f)
    }

    val textColor = when {
        !isInternetAvailable -> Color(0xFFF44336)
        !isConnected -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        !isConnected -> Icons.Default.Wifi
                        else -> Icons.Default.Wifi
                    },
                    contentDescription = "Estado de conexión",
                    tint = textColor
                )
                Column {
                    Text(
                        text = connectionStatus,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when {
                            !isInternetAvailable -> "Los cambios se guardarán localmente"
                            !isConnected -> "Reconectando con la cocina..."
                            else -> "Comunicación en tiempo real con cocina"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                    )
                }
            }

            if (!isConnected && isInternetAvailable) {
                TextButton(
                    onClick = onSyncClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF9800))
                ) {
                    Text("Reconectar")
                }
            }
        }
    }
}

@Composable
fun OrdersSummary(
    totalOrders: Int,
    pendingOrders: Int,
    inProgressOrders: Int,
    readyOrders: Int,
    isFirebaseConnected: Boolean,
    isInternetAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a2e).copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Resumen de Órdenes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = when {
                        !isInternetAvailable -> "🔴 Modo offline"
                        isFirebaseConnected -> "🟢 En vivo"
                        else -> "🟡 Conectando..."
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        !isInternetAvailable -> Color(0xFFF44336)
                        isFirebaseConnected -> Color(0xFF4CAF50)
                        else -> Color(0xFFFF9800)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                OrdersStatItem(
                    count = totalOrders,
                    label = "Total",
                    color = Color(0xFF2196F3)
                )
                OrdersStatItem(
                    count = pendingOrders,
                    label = "Pendientes",
                    color = Color(0xFFFF9800)
                )
                OrdersStatItem(
                    count = inProgressOrders,
                    label = "En Prep.",
                    color = Color(0xFFFF5722)
                )
                OrdersStatItem(
                    count = readyOrders,
                    label = "Listas",
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
fun OrdersStatItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
    }
}

// ✅ ACTUALIZADO: OrdersListContent con los nuevos estados
@Composable
fun OrdersListContent(
    orders: List<com.laprevia.restobar.data.model.Order>,
    viewModel: WaiterViewModel
) {
    val ordersByStatus = orders.groupBy { it.status }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        // 🎉 SECCIÓN 1: Órdenes LISTAS (comida lista para llevar)
        ordersByStatus[OrderStatus.LISTO]?.let { readyOrders ->
            if (readyOrders.isNotEmpty()) {
                item {
                    OrdersSectionHeader(
                        title = "🎉 LISTAS PARA SERVIR",
                        count = readyOrders.size,
                        color = Color(0xFF2196F3),
                        description = "Comida lista - Presiona ENTREGAR para llevar al cliente"
                    )
                }
                items(readyOrders.sortedBy { it.createdAt }) { order ->
                    OrderCard(
                        order = order,
                        onMarkAsDelivered = {
                            println("🍽️ Entregando comida orden: ${order.id}")
                            viewModel.markOrderAsDelivered(order.id)
                        }
                    )
                }
            }
        }

        // 🍽️ SECCIÓN 2: Órdenes ENTREGADAS (cliente comiendo)
        ordersByStatus[OrderStatus.ENTREGADO]?.let { deliveredOrders ->
            if (deliveredOrders.isNotEmpty()) {
                item {
                    OrdersSectionHeader(
                        title = "🍽️ COMIDA ENTREGADA",
                        count = deliveredOrders.size,
                        color = Color(0xFFFF9800),
                        description = "Cliente comiendo - Presiona LIBERAR cuando se vayan"
                    )
                }
                items(deliveredOrders.sortedBy { it.createdAt }) { order ->
                    OrderCard(
                        order = order,
                        onMarkAsServed = {
                            println("🧹 Liberando mesa orden: ${order.id}")
                            viewModel.markTableAsFree(order.id)
                        }
                    )
                }
            }
        }

        // 👨‍🍳 SECCIÓN 3: Órdenes EN PREPARACIÓN
        ordersByStatus[OrderStatus.EN_PREPARACION]?.let { inProgressOrders ->
            if (inProgressOrders.isNotEmpty()) {
                item {
                    OrdersSectionHeader(
                        title = "👨‍🍳 EN PREPARACIÓN",
                        count = inProgressOrders.size,
                        color = Color(0xFFFF5722),
                        description = "La cocina está preparando"
                    )
                }
                items(inProgressOrders.sortedBy { it.createdAt }) { order ->
                    OrderCard(order = order)
                }
            }
        }

        // ✅ SECCIÓN 4: Órdenes ACEPTADAS
        ordersByStatus[OrderStatus.ACEPTADO]?.let { acceptedOrders ->
            if (acceptedOrders.isNotEmpty()) {
                item {
                    OrdersSectionHeader(
                        title = "✅ ACEPTADAS",
                        count = acceptedOrders.size,
                        color = Color(0xFF9C27B0),
                        description = "Cocina aceptó, próximamente en preparación"
                    )
                }
                items(acceptedOrders.sortedBy { it.createdAt }) { order ->
                    OrderCard(order = order)
                }
            }
        }

        // 📤 SECCIÓN 5: Órdenes ENVIADAS
        ordersByStatus[OrderStatus.ENVIADO]?.let { sentOrders ->
            if (sentOrders.isNotEmpty()) {
                item {
                    OrdersSectionHeader(
                        title = "📤 ENVIADAS A COCINA",
                        count = sentOrders.size,
                        color = Color(0xFF2196F3),
                        description = "Esperando confirmación de la cocina"
                    )
                }
                items(sentOrders.sortedBy { it.createdAt }) { order ->
                    OrderCard(order = order)
                }
            }
        }
    }
}

@Composable
fun OrdersSectionHeader(
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
            CircularProgressIndicator(color = Color.White)
            Text(
                text = if (!isInternetAvailable) "Sin conexión - Cargando datos locales..." else "Conectando con cocina...",
                color = Color.White
            )
        }
    }
}

@Composable
fun EmptyOrdersState(
    isFirebaseConnected: Boolean,
    isInternetAvailable: Boolean,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = when {
                    !isInternetAvailable -> Icons.Default.WifiOff
                    else -> Icons.Default.Restaurant
                },
                contentDescription = "Sin órdenes",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = when {
                    !isInternetAvailable -> "Sin conexión a internet"
                    isFirebaseConnected -> "No hay órdenes activas"
                    else -> "Conectando con el servidor"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = when {
                    !isInternetAvailable -> "Las órdenes se sincronizarán cuando vuelva internet"
                    isFirebaseConnected -> "Las órdenes creadas aparecerán aquí automáticamente"
                    else -> "Esperando conexión con el servidor"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
            if (!isInternetAvailable) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFe94560))
                ) {
                    Text("Reintentar Conexión")
                }
            }
        }
    }
}
package com.laprevia.restobar.presentation.screens.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.presentation.screens.waiter.components.OrderCard
import com.laprevia.restobar.presentation.viewmodel.WaiterViewModel

@Composable
fun OrdersScreen(
    navController: NavController,
    viewModel: WaiterViewModel = hiltViewModel()
) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    // 🔥 NUEVO: Sincronizar con Firebase automáticamente
    LaunchedEffect(isFirebaseConnected) {
        if (!isFirebaseConnected) {
            viewModel.syncWithFirebase()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0f3460))
    ) {
        // 🔥 NUEVO: Banner de conexión mejorado
        FirebaseConnectionBanner(
            isConnected = isFirebaseConnected,
            connectionStatus = connectionStatus,
            isLoading = isLoading,
            onSyncClick = { viewModel.syncWithFirebase() }
        )

        // Header con estadísticas
        OrdersSummary(
            totalOrders = orders.size,
            pendingOrders = orders.count { it.status == OrderStatus.ENVIADO },
            inProgressOrders = orders.count { it.status == OrderStatus.EN_PREPARACION },
            readyOrders = orders.count { it.status == OrderStatus.LISTO },
            isFirebaseConnected = isFirebaseConnected, // 🔥 NUEVO
            modifier = Modifier.padding(16.dp)
        )

        if (isLoading && orders.isEmpty()) {
            LoadingState()
        } else if (orders.isEmpty()) {
            EmptyOrdersState(isFirebaseConnected = isFirebaseConnected)
        } else {
            OrdersListContent(
                orders = orders,
                viewModel = viewModel // 🔥 NUEVO: Pasar el viewModel
            )
        }
    }
}

// 🔥 NUEVO: Banner de conexión con Firebase
@Composable
fun FirebaseConnectionBanner(
    isConnected: Boolean,
    connectionStatus: String,
    isLoading: Boolean,
    onSyncClick: () -> Unit
) {
    if (isLoading) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected) Color(0xFF4CAF50).copy(alpha = 0.1f)
            else Color(0xFFF44336).copy(alpha = 0.1f)
        )
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
                    imageVector = if (isConnected) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = "Estado de conexión",
                    tint = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Column {
                    Text(
                        text = connectionStatus,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isConnected) "Comunicación en tiempo real con cocina"
                        else "Los cambios pueden no sincronizarse",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                    )
                }
            }

            if (!isConnected) {
                TextButton(
                    onClick = onSyncClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFF44336)
                    )
                ) {
                    Text("Reintentar")
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
    isFirebaseConnected: Boolean, // 🔥 NUEVO
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

                // 🔥 NUEVO: Indicador de sincronización
                Text(
                    text = if (isFirebaseConnected) "🟢 En vivo" else "🔴 Local",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isFirebaseConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
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

@Composable
fun OrdersListContent(
    orders: List<com.laprevia.restobar.data.model.Order>,
    viewModel: WaiterViewModel // 🔥 NUEVO: Recibir el viewModel
) {
    // Agrupar órdenes por estado para mostrar secciones
    val ordersByStatus = orders.groupBy { it.status }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        // 🔥 NUEVO: Órdenes LISTAS (ALTA PRIORIDAD - aparecen primero)
        ordersByStatus[OrderStatus.LISTO]?.let { readyOrders ->
            if (readyOrders.isNotEmpty()) {
                item {
                    OrdersSectionHeader(
                        title = "🎉 Listas para Servir",
                        count = readyOrders.size,
                        color = Color(0xFF4CAF50),
                        description = "Órdenes listas para entregar a los clientes"
                    )
                }
                items(readyOrders.sortedBy { it.createdAt }) { order ->
                    OrderCard(
                        order = order,
                        // 🔥 CORREGIDO: Ahora sí llama al método
                        onMarkAsServed = {
                            println("🖱️ OrdersScreen: Marcando orden ${order.id} como servida")
                            viewModel.markOrderAsServed(order.id)
                        }
                    )
                }
            }
        }

        // 🔥 NUEVO: Órdenes EN PREPARACIÓN
        ordersByStatus[OrderStatus.EN_PREPARACION]?.let { inProgressOrders ->
            if (inProgressOrders.isNotEmpty()) {
                item {
                    OrdersSectionHeader(
                        title = "👨‍🍳 En Preparación",
                        count = inProgressOrders.size,
                        color = Color(0xFFFF9800),
                        description = "La cocina está preparando estas órdenes"
                    )
                }
                items(inProgressOrders.sortedBy { it.createdAt }) { order ->
                    OrderCard(
                        order = order,
                        onMarkAsServed = {} // Para órdenes no listas, función vacía
                    )
                }
            }
        }

        // 🔥 NUEVO: Órdenes ACEPTADAS
        ordersByStatus[OrderStatus.ACEPTADO]?.let { acceptedOrders ->
            if (acceptedOrders.isNotEmpty()) {
                item {
                    OrdersSectionHeader(
                        title = "✅ Aceptadas por Cocina",
                        count = acceptedOrders.size,
                        color = Color(0xFF9C27B0),
                        description = "Órdenes aceptadas, pendientes de preparación"
                    )
                }
                items(acceptedOrders.sortedBy { it.createdAt }) { order ->
                    OrderCard(
                        order = order,
                        onMarkAsServed = {} // Para órdenes no listas, función vacía
                    )
                }
            }
        }

        // 🔥 NUEVO: Órdenes ENVIADAS
        ordersByStatus[OrderStatus.ENVIADO]?.let { sentOrders ->
            if (sentOrders.isNotEmpty()) {
                item {
                    OrdersSectionHeader(
                        title = "📤 Enviadas a Cocina",
                        count = sentOrders.size,
                        color = Color(0xFF2196F3),
                        description = "Esperando confirmación de la cocina"
                    )
                }
                items(sentOrders.sortedBy { it.createdAt }) { order ->
                    OrderCard(
                        order = order,
                        onMarkAsServed = {} // Para órdenes no listas, función vacía
                    )
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
    description: String = "" // 🔥 NUEVO: Descripción opcional
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

            // 🔥 NUEVO: Descripción de la sección
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
fun LoadingState() {
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
                "Conectando con cocina...",
                color = Color.White
            )
        }
    }
}

@Composable
fun EmptyOrdersState(isFirebaseConnected: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = "Sin órdenes",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = if (isFirebaseConnected) {
                    "No hay órdenes activas"
                } else {
                    "Modo local activado"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                text = if (isFirebaseConnected) {
                    "Las órdenes creadas aparecerán aquí automáticamente"
                } else {
                    "Las órdenes se sincronizarán cuando se restablezca la conexión"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
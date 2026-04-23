// OrdersScreen.kt - VERSIÓN CORREGIDA
package com.laprevia.restobar.presentation.screens.chef

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.presentation.screens.chef.components.OrderCard
import com.laprevia.restobar.presentation.screens.chef.components.QuickAction
import com.laprevia.restobar.presentation.viewmodel.ChefViewModel


@Composable
fun OrdersScreen(
    viewModel: ChefViewModel = hiltViewModel()
) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshOrders()
    }

    // 🔥 NUEVO: Mostrar notificaciones automáticamente
    LaunchedEffect(notifications) {
        if (notifications.isNotEmpty()) {
            // Aquí podrías agregar un sistema de snackbars o toasts
            println("📢 Notificaciones del chef: ${notifications.size}")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // 🔥 NUEVO: Banner de estado de conexión
        ConnectionStatusBanner(
            isConnected = isFirebaseConnected,
            isLoading = isLoading
        )

        // Agrupar órdenes por estado
        val ordersByStatus = orders.groupBy { it.status }
        val sentOrders = ordersByStatus[OrderStatus.ENVIADO] ?: emptyList()
        val acceptedOrders = ordersByStatus[OrderStatus.ACEPTADO] ?: emptyList()
        val inPreparationOrders = ordersByStatus[OrderStatus.EN_PREPARACION] ?: emptyList()
        val readyOrders = ordersByStatus[OrderStatus.LISTO] ?: emptyList()

        if (isLoading && orders.isEmpty()) {
            LoadingState()
        } else if (orders.isEmpty()) {
            EmptyChefOrdersState(isFirebaseConnected = isFirebaseConnected)
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
fun ConnectionStatusBanner(
    isConnected: Boolean,
    isLoading: Boolean
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                Text(
                    text = if (isConnected) "🟢 Conectado a meseros" else "🔴 Sin conexión con meseros",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.Medium
                )
            }

            if (!isConnected) {
                Text(
                    text = "Modo local",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun OrdersList(
    sentOrders: List<Order>, // ✅ CORREGIDO: Usar Order directamente
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
        // 🔥 NUEVO: Resumen rápido de órdenes
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
                    // 🔥 NUEVO: Acciones rápidas
                    quickActions = listOf(
                        QuickAction( // ✅ CORREGIDO: Usar QuickAction del mismo paquete
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
                        // 🔥 NUEVO: Marcar como completada (opcional)
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
fun LoadingState() {
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
                text = "Conectando con cocina...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun EmptyChefOrdersState(isFirebaseConnected: Boolean) {
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
                contentDescription = "Sin pedidos",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = if (isFirebaseConnected) {
                    "No hay pedidos pendientes"
                } else {
                    "Modo local activado"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = if (isFirebaseConnected) {
                    "Los pedidos enviados por los meseros aparecerán aquí automáticamente"
                } else {
                    "Los pedidos se cargarán cuando se restablezca la conexión"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                textAlign = TextAlign.Center // ✅ CORREGIDO: Import correcto
            )

        }
    }
}
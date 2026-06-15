package com.laprevia.restobar.presentation.screens.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.presentation.screens.waiter.components.OrderCard
import com.laprevia.restobar.presentation.theme.CoralSecondary
import com.laprevia.restobar.presentation.theme.ErrorRed
import com.laprevia.restobar.presentation.theme.InfoBlue
import com.laprevia.restobar.presentation.theme.NightBackground
import com.laprevia.restobar.presentation.theme.NightSurface
import com.laprevia.restobar.presentation.theme.SmokeWhite
import com.laprevia.restobar.presentation.theme.SmokeWhiteDisabled
import com.laprevia.restobar.presentation.theme.SmokeWhiteSecondary
import com.laprevia.restobar.presentation.theme.SuccessGreen
import com.laprevia.restobar.presentation.theme.WarningOrange
import com.laprevia.restobar.presentation.viewmodel.WaiterViewModel
import kotlinx.coroutines.delay
import timber.log.Timber

// ────────────────────────────────────────────────────────────
// Indicador de estado (punto de color con texto)
// ────────────────────────────────────────────────────────────

@Composable
private fun StatusDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

// ────────────────────────────────────────────────────────────
// Pantalla principal de Órdenes (Waiter)
// ────────────────────────────────────────────────────────────

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

    val connectionStatusColor = when {
        !isInternetAvailable -> ErrorRed
        isFirebaseConnected -> SuccessGreen
        else -> WarningOrange
    }

    val connectionStatusText = when {
        !isInternetAvailable -> "Sin internet — Modo offline"
        isFirebaseConnected -> "Conectado a cocina"
        else -> "Reconectando..."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NightBackground)
    ) {
        FirebaseConnectionBanner(
            isConnected = isFirebaseConnected,
            isInternetAvailable = isInternetAvailable,
            connectionStatusText = connectionStatusText,
            connectionStatusColor = connectionStatusColor,
            isLoading = isLoading,
            onSyncClick = { viewModel.syncWithFirebase() }
        )

        if (currentConnectionMessage != null) {
            val isError = currentConnectionMessage.contains("SIN INTERNET")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isError)
                        WarningOrange.copy(alpha = 0.9f)
                    else
                        SuccessGreen.copy(alpha = 0.9f)
                )
            ) {
                Text(
                    text = currentConnectionMessage,
                    color = SmokeWhite,
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
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = WarningOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Modo offline — Mostrando órdenes guardadas localmente",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningOrange
                    )
                }
            }
            OrdersListContent(
                orders = orders,
                viewModel = viewModel
            )
        }
    }
}

// ────────────────────────────────────────────────────────────
// Banner de conexión Firebase
// ────────────────────────────────────────────────────────────

@Composable
fun FirebaseConnectionBanner(
    isConnected: Boolean,
    isInternetAvailable: Boolean,
    connectionStatusText: String,
    connectionStatusColor: Color,
    isLoading: Boolean,
    onSyncClick: () -> Unit
) {
    if (isLoading) return

    val backgroundColor = when {
        !isInternetAvailable -> ErrorRed.copy(alpha = 0.15f)
        !isConnected -> WarningOrange.copy(alpha = 0.15f)
        else -> SuccessGreen.copy(alpha = 0.15f)
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
                StatusDot(color = connectionStatusColor)
                Icon(
                    imageVector = when {
                        !isInternetAvailable -> Icons.Default.WifiOff
                        else -> Icons.Default.Wifi
                    },
                    contentDescription = "Estado de conexión",
                    tint = connectionStatusColor,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = connectionStatusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = connectionStatusColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = when {
                            !isInternetAvailable -> "Los cambios se guardarán localmente"
                            !isConnected -> "Reconectando con la cocina..."
                            else -> "Comunicación en tiempo real con cocina"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = connectionStatusColor
                    )
                }
            }

            if (!isConnected && isInternetAvailable) {
                TextButton(
                    onClick = onSyncClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = WarningOrange)
                ) {
                    Text("Reconectar")
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────
// Resumen de órdenes
// ────────────────────────────────────────────────────────────

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
            containerColor = NightSurface.copy(alpha = 0.6f)
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
                    color = SmokeWhite
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusDot(
                        color = when {
                            !isInternetAvailable -> ErrorRed
                            isFirebaseConnected -> SuccessGreen
                            else -> WarningOrange
                        }
                    )
                    Text(
                        text = when {
                            !isInternetAvailable -> "Modo offline"
                            isFirebaseConnected -> "En vivo"
                            else -> "Conectando..."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            !isInternetAvailable -> ErrorRed
                            isFirebaseConnected -> SuccessGreen
                            else -> WarningOrange
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                OrdersStatItem(count = totalOrders, label = "Total", color = InfoBlue)
                OrdersStatItem(count = pendingOrders, label = "Pendientes", color = WarningOrange)
                OrdersStatItem(count = inProgressOrders, label = "En Prep.", color = CoralSecondary)
                OrdersStatItem(count = readyOrders, label = "Listas", color = SuccessGreen)
            }
        }
    }
}

// ────────────────────────────────────────────────────────────
// Ítem individual de estadística
// ────────────────────────────────────────────────────────────

@Composable
fun OrdersStatItem(
    count: Int,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = SmokeWhiteSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

// ────────────────────────────────────────────────────────────
// Lista de órdenes agrupadas por estado
// ────────────────────────────────────────────────────────────

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
        // Sección 1: LISTAS PARA SERVIR
        ordersByStatus[OrderStatus.LISTO]?.let { readyOrders ->
            if (readyOrders.isNotEmpty()) {
                item {
                    OrdersSectionHeader(
                        title = "Listas para servir",
                        icon = Icons.Default.Celebration,
                        count = readyOrders.size,
                        color = InfoBlue,
                        description = "Comida lista — Presiona ENTREGAR para llevar al cliente"
                    )
                }
                items(readyOrders.sortedBy { it.createdAt }) { order ->
                    OrderCard(
                        order = order,
                        onMarkAsDelivered = {
                            Timber.d("🍽️ Entregando comida orden: ${order.id}")
                            viewModel.markOrderAsDelivered(order.id)
                        },
                        onCancel = {
                            Timber.d("❌ Cancelando orden: ${order.id}")
                            viewModel.cancelOrder(order.id)
                        }
                    )
                }
            }
        }

        // Sección 2: ENTREGADAS
        ordersByStatus[OrderStatus.ENTREGADO]?.let { deliveredOrders ->
            if (deliveredOrders.isNotEmpty()) {
                item {
                    OrdersSectionHeader(
                        title = "Comida entregada",
                        icon = Icons.Default.RestaurantMenu,
                        count = deliveredOrders.size,
                        color = WarningOrange,
                        description = "Cliente comiendo — Presiona LIBERAR cuando se vayan"
                    )
                }
                items(deliveredOrders.sortedBy { it.createdAt }) { order ->
                    OrderCard(
                        order = order,
                        onMarkAsServed = {
                            Timber.d("🧹 Liberando mesa orden: ${order.id}")
                            viewModel.markTableAsFree(order.id)
                        },
                        onCancel = {
                            Timber.d("❌ Cancelando orden: ${order.id}")
                            viewModel.cancelOrder(order.id)
                        }
                    )
                }
            }
        }

        // Sección 3: EN PREPARACIÓN
        ordersByStatus[OrderStatus.EN_PREPARACION]?.let { inProgressOrders ->
            if (inProgressOrders.isNotEmpty()) {
                item {
                    OrdersSectionHeader(
                        title = "En preparación",
                        icon = Icons.Default.OutdoorGrill,
                        count = inProgressOrders.size,
                        color = CoralSecondary,
                        description = "La cocina está preparando"
                    )
                }
                items(inProgressOrders.sortedBy { it.createdAt }) { order ->
                    OrderCard(
                        order = order,
                        onCancel = {
                            Timber.d("❌ Cancelando orden: ${order.id}")
                            viewModel.cancelOrder(order.id)
                        }
                    )
                }
            }
        }

        // Sección 4: ACEPTADAS
        ordersByStatus[OrderStatus.ACEPTADO]?.let { acceptedOrders ->
            if (acceptedOrders.isNotEmpty()) {
                item {
                    OrdersSectionHeader(
                        title = "Aceptadas",
                        icon = Icons.Default.CheckCircle,
                        count = acceptedOrders.size,
                        color = CoralSecondary.copy(alpha = 0.8f),
                        description = "Cocina aceptó, próximamente en preparación"
                    )
                }
                items(acceptedOrders.sortedBy { it.createdAt }) { order ->
                    OrderCard(
                        order = order,
                        onCancel = {
                            Timber.d("❌ Cancelando orden: ${order.id}")
                            viewModel.cancelOrder(order.id)
                        }
                    )
                }
            }
        }

        // Sección 5: ENVIADAS A COCINA
        ordersByStatus[OrderStatus.ENVIADO]?.let { sentOrders ->
            if (sentOrders.isNotEmpty()) {
                item {
                    OrdersSectionHeader(
                        title = "Enviadas a cocina",
                        icon = Icons.Default.Send,
                        count = sentOrders.size,
                        color = InfoBlue,
                        description = "Esperando confirmación de la cocina"
                    )
                }
                items(sentOrders.sortedBy { it.createdAt }) { order ->
                    OrderCard(
                        order = order,
                        onCancel = {
                            Timber.d("❌ Cancelando orden: ${order.id}")
                            viewModel.cancelOrder(order.id)
                        }
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────
// Encabezado de sección con ícono vectorial
// ────────────────────────────────────────────────────────────

@Composable
fun OrdersSectionHeader(
    title: String,
    icon: ImageVector,
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }

                Surface(
                    color = color,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = count.toString(),
                        color = SmokeWhite,
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

// ────────────────────────────────────────────────────────────
// Estado de carga
// ────────────────────────────────────────────────────────────

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
            CircularProgressIndicator(color = CoralSecondary)
            Text(
                text = if (!isInternetAvailable) "Sin conexión — Cargando datos locales..." else "Conectando con cocina...",
                color = SmokeWhite
            )
        }
    }
}

// ────────────────────────────────────────────────────────────
// Estado vacío (sin órdenes)
// ────────────────────────────────────────────────────────────

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
                    else -> Icons.Default.RestaurantMenu
                },
                contentDescription = "Sin órdenes",
                tint = SmokeWhiteDisabled,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = when {
                    !isInternetAvailable -> "Sin conexión a internet"
                    isFirebaseConnected -> "No hay órdenes activas"
                    else -> "Conectando con el servidor"
                },
                style = MaterialTheme.typography.titleMedium,
                color = SmokeWhiteSecondary
            )
            Text(
                text = when {
                    !isInternetAvailable -> "Las órdenes se sincronizarán cuando vuelva internet"
                    isFirebaseConnected -> "Las órdenes creadas aparecerán aquí automáticamente"
                    else -> "Esperando conexión con el servidor"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = SmokeWhiteDisabled,
                textAlign = TextAlign.Center
            )
            if (!isInternetAvailable) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = CoralSecondary)
                ) {
                    Text("Reintentar Conexión")
                }
            }
        }
    }
}

package com.laprevia.restobar.presentation.screens.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.laprevia.restobar.presentation.screens.waiter.components.NotificationPanel
import com.laprevia.restobar.presentation.viewmodel.WaiterViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaiterMainScreen(
    navController: NavController,
    viewModel: WaiterViewModel = hiltViewModel(),
    onLogout: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showNotifications by remember { mutableStateOf(false) }

    // Estados actualizados
    val notifications by viewModel.notifications.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val tables by viewModel.tables.collectAsState()
    val products by viewModel.products.collectAsState()
    val currentOrderItems by viewModel.currentOrderItems.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()
    val isInternetAvailable by viewModel.isInternetAvailable.collectAsState()
    val connectionMessage by viewModel.connectionMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // Auto-clear mensajes después de 3 segundos
    LaunchedEffect(successMessage, errorMessage, connectionMessage) {
        if (successMessage != null || errorMessage != null || connectionMessage != null) {
            delay(3000)
            if (successMessage != null) viewModel.clearSuccessMessage()
            if (errorMessage != null) viewModel.clearError()
            if (connectionMessage != null) viewModel.clearConnectionMessage()
        }
    }

    val backgroundColor = Color(0xFF0f3460)
    val surfaceColor = Color(0xFF1a1a2e)
    val accentColor = Color(0xFFe94560)

    val readyOrdersCount = orders.count { it.status == com.laprevia.restobar.data.model.OrderStatus.LISTO }
    val occupiedTablesCount = tables.count { it.status == com.laprevia.restobar.data.model.TableStatus.OCUPADA }
    val totalItemsInCart = currentOrderItems.sumOf { it.quantity }

    // Texto de estado de conexión mejorado
    val connectionStatusText = when {
        !isInternetAvailable -> "🔴 SIN INTERNET - Modo offline"
        isFirebaseConnected -> "🟢 Conectado a cocina"
        else -> "🟡 Conectando..."
    }

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    ),
                color = Color(0xFF1a1a2e),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(
                                        SpanStyle(
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 24.sp,
                                            letterSpacing = 2.sp
                                        )
                                    ) {
                                        append("LA PREVIA\n")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = Color(0xFFe94560),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            letterSpacing = 3.sp
                                        )
                                    ) {
                                        append("RESTOBAR")
                                    }
                                }
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                IconButton(
                                    onClick = { showNotifications = !showNotifications },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF16213e))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notificaciones",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (notifications.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .align(Alignment.TopEnd)
                                            .clip(CircleShape)
                                            .background(Color(0xFFe94560)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (notifications.size > 9) "9+" else notifications.size.toString(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFe94560))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Cerrar sesión",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Banner de estado de conexión
                    ConnectionStatusBannerWaiter(
                        isInternetAvailable = isInternetAvailable,
                        isFirebaseConnected = isFirebaseConnected,
                        connectionStatusText = connectionStatusText,
                        onManualSync = { viewModel.syncWithFirebase() }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SISTEMA MESERO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 3.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (readyOrdersCount > 0) {
                                Badge(containerColor = Color(0xFFe94560)) {
                                    Text(
                                        text = "$readyOrdersCount listas",
                                        color = Color.White,
                                        fontSize = 9.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "${tables.size} mesas | $occupiedTablesCount ocup.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF0f3460)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(backgroundColor, surfaceColor)
                    )
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Mensajes temporales
                MessageBannerWaiter(
                    error = errorMessage,
                    warning = connectionMessage,
                    success = successMessage,
                    onClearError = { viewModel.clearError() },
                    onClearWarning = { viewModel.clearConnectionMessage() },
                    onClearSuccess = { viewModel.clearSuccessMessage() }
                )

                // Panel de bienvenida
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(
                        containerColor = surfaceColor.copy(alpha = 0.8f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(accentColor, surfaceColor)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestaurantMenu,
                                contentDescription = "Mesero",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "¡Bienvenido, Mesero!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Gestiona mesas y pedidos conectado a cocina",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )

                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = connectionStatusText,
                                    color = when {
                                        !isInternetAvailable -> Color(0xFFF44336)
                                        isFirebaseConnected -> Color(0xFF4CAF50)
                                        else -> Color(0xFFFF9800)
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Text(
                                    text = "•",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp
                                )

                                Text(
                                    text = "${tables.size} mesas",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )

                                Text(
                                    text = "•",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp
                                )

                                Text(
                                    text = "$occupiedTablesCount ocup.",
                                    color = if (occupiedTablesCount > 0) Color(0xFFFFA000) else Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = if (occupiedTablesCount > 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }

                            if (readyOrdersCount > 0) {
                                Text(
                                    text = "$readyOrdersCount órdenes listas para servir",
                                    color = Color(0xFFe94560),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Panel de notificaciones
                if (showNotifications && notifications.isNotEmpty()) {
                    NotificationPanel(
                        notifications = notifications,
                        onDismissNotification = { notification ->
                            viewModel.removeNotification(notification)
                        },
                        onClearAll = {
                            viewModel.clearAllNotifications()
                            showNotifications = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Tabs principales
                Column(modifier = Modifier.fillMaxSize()) {
                    if (totalItemsInCart > 0) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "🛒 $totalItemsInCart items en carrito",
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = surfaceColor.copy(alpha = 0.6f),
                        contentColor = Color.White,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                showNotifications = false
                            },
                            text = { Text("MESAS", fontWeight = FontWeight.Medium, fontSize = 14.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                showNotifications = false
                            },
                            text = { Text("PRODUCTOS", fontWeight = FontWeight.Medium, fontSize = 14.sp) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = {
                                selectedTab = 2
                                showNotifications = false
                            },
                            text = { Text("ÓRDENES", fontWeight = FontWeight.Medium, fontSize = 14.sp) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        when (selectedTab) {
                            0 -> TablesScreen(navController = navController, viewModel = viewModel)
                            1 -> ProductsScreen(navController = navController, viewModel = viewModel)
                            2 -> OrdersScreen(navController = navController, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusBannerWaiter(
    isInternetAvailable: Boolean,
    isFirebaseConnected: Boolean,
    connectionStatusText: String,
    onManualSync: () -> Unit
) {
    val backgroundColor = when {
        !isInternetAvailable -> Color(0xFFF44336).copy(alpha = 0.9f)
        !isFirebaseConnected -> Color(0xFFFF9800).copy(alpha = 0.9f)
        else -> Color(0xFF4CAF50).copy(alpha = 0.9f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        !isInternetAvailable -> Icons.Default.WifiOff
                        !isFirebaseConnected -> Icons.Default.Sync
                        else -> Icons.Default.Wifi
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = connectionStatusText,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!isFirebaseConnected && isInternetAvailable) {
                TextButton(
                    onClick = onManualSync,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Reconectar", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                }
            }
        }
    }
}

@Composable
fun MessageBannerWaiter(
    error: String?,
    warning: String?,
    success: String?,
    onClearError: () -> Unit,
    onClearWarning: () -> Unit,
    onClearSuccess: () -> Unit
) {
    if (error != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336).copy(alpha = 0.95f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Error, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(error, color = Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = onClearError, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (warning != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.95f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(warning, color = Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = onClearWarning, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }

    if (success != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.95f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(success, color = Color.White, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = onClearSuccess, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
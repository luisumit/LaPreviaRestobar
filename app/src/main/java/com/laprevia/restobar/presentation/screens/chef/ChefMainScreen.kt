package com.laprevia.restobar.presentation.screens.chef

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
import com.laprevia.restobar.presentation.screens.chef.components.ChefNotificationPanel
import com.laprevia.restobar.presentation.viewmodel.ChefViewModel
import com.laprevia.restobar.presentation.viewmodel.InventoryViewModel
import com.laprevia.restobar.presentation.viewmodel.LoginViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefMainScreen(
    chefViewModel: ChefViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel(),
    inventoryViewModel: InventoryViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    // Estados
    var selectedTab by remember { mutableStateOf(0) }
    var showNotifications by remember { mutableStateOf(false) }

    // Estados del ViewModel actualizados
    val notifications by remember { chefViewModel.notifications }.collectAsState()
    val orders by remember { chefViewModel.orders }.collectAsState()
    val successMessage by remember { chefViewModel.successMessage }.collectAsState()
    val errorMessage by remember { chefViewModel.errorMessage }.collectAsState()
    val connectionMessage by remember { chefViewModel.connectionMessage }.collectAsState()
    val isFirebaseConnected by remember { chefViewModel.isFirebaseConnected }.collectAsState()
    val isInternetAvailable by remember { chefViewModel.isInternetAvailable }.collectAsState()

    // Texto de estado de conexión
    val connectionStatusText = when {
        !isInternetAvailable -> "🔴 SIN INTERNET - Modo offline"
        isFirebaseConnected -> "🟢 Conectado a Firebase"
        else -> "🟡 Conectando..."
    }

    // Auto-clear mensajes después de 3 segundos
    LaunchedEffect(successMessage, errorMessage, connectionMessage) {
        if (successMessage != null || errorMessage != null || connectionMessage != null) {
            delay(3000)
            if (successMessage != null) chefViewModel.clearSuccessMessage()
            if (errorMessage != null) chefViewModel.clearError()
            if (connectionMessage != null) chefViewModel.clearConnectionMessage()
        }
    }

    // Contadores
    val newOrdersCount = orders.count { it.status == com.laprevia.restobar.data.model.OrderStatus.ENVIADO }
    val activeOrdersCount = orders.size

    val backgroundColor = Color(0xFF0f3460)
    val surfaceColor = Color(0xFF1a1a2e)
    val accentColor = Color(0xFFe94560)

    // Cargar órdenes al iniciar
    LaunchedEffect(Unit) {
        chefViewModel.refreshOrders()
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
                            BadgedBox(
                                badge = {
                                    if (notifications.isNotEmpty()) {
                                        Badge { Text(text = notifications.size.toString()) }
                                    }
                                }
                            ) {
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
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    println("🔄 ChefScreen: Cerrando sesión...")
                                    loginViewModel.signOut()
                                    onLogout()
                                },
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

                    // ✅ BANNER DE ESTADO DE CONEXIÓN
                    ConnectionStatusBannerChefMain(
                        isInternetAvailable = isInternetAvailable,
                        isFirebaseConnected = isFirebaseConnected,
                        connectionStatusText = connectionStatusText,
                        onManualSync = { chefViewModel.manualSync() }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SISTEMA COCINERO",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 3.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (newOrdersCount > 0) {
                                Badge(containerColor = Color(0xFFe94560)) {
                                    Text(text = "$newOrdersCount nuevas")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "$activeOrdersCount activas",
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
                // ✅ MENSAJES TEMPORALES
                MessageBannerChefMain(
                    error = errorMessage,
                    warning = connectionMessage,
                    success = successMessage,
                    onClearError = { chefViewModel.clearError() },
                    onClearWarning = { chefViewModel.clearConnectionMessage() },
                    onClearSuccess = { chefViewModel.clearSuccessMessage() }
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
                                contentDescription = "Chef",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "¡Bienvenido, Chef!",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Controla pedidos e inventario conectado al sistema",
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
                                    text = "$activeOrdersCount activas",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }

                            if (newOrdersCount > 0) {
                                Text(
                                    text = "$newOrdersCount nuevas órdenes pendientes",
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
                    ChefNotificationPanel(
                        notifications = notifications,
                        onDismissNotification = { notification ->
                            chefViewModel.removeNotification(notification)
                        },
                        onClearAll = {
                            chefViewModel.clearAllNotifications()
                            showNotifications = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Tabs principales
                Column(modifier = Modifier.fillMaxSize()) {
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
                            text = { Text(text = "ÓRDENES") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                showNotifications = false
                            },
                            text = { Text(text = "INVENTARIO") }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        when (selectedTab) {
                            0 -> OrdersScreen(viewModel = chefViewModel)
                            1 -> InventoryScreen(viewModel = inventoryViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionStatusBannerChefMain(
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
fun MessageBannerChefMain(
    error: String?,
    warning: String?,
    success: String?,
    onClearError: () -> Unit,
    onClearWarning: () -> Unit,
    onClearSuccess: () -> Unit
) {
    // Mensaje de ERROR (rojo)
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

    // Mensaje de ADVERTENCIA (naranja)
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

    // Mensaje de ÉXITO (verde)
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
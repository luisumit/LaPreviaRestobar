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
import androidx.compose.ui.platform.LocalConfiguration
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

    // Detectar tamaño de pantalla para diseño responsivo
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

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

    // Colores responsivos según tamaño
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

    // Padding responsivo
    val horizontalPadding = if (isTablet) 32.dp else 16.dp
    val topBarPadding = if (isTablet) 24.dp else 16.dp

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isTablet) 8.dp else 4.dp,
                        shape = RoundedCornerShape(bottomStart = if (isTablet) 32.dp else 24.dp, bottomEnd = if (isTablet) 32.dp else 24.dp)
                    ),
                color = Color(0xFF1a1a2e),
                contentColor = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = topBarPadding, horizontal = horizontalPadding)
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
                                            fontSize = if (isTablet) 32.sp else 24.sp,
                                            letterSpacing = 2.sp
                                        )
                                    ) {
                                        append("LA PREVIA\n")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = Color(0xFFe94560),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (isTablet) 20.sp else 16.sp,
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
                                        .size(if (isTablet) 52.dp else 44.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF16213e))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notificaciones",
                                        tint = Color.White,
                                        modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                                    )
                                }

                                if (notifications.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isTablet) 22.dp else 18.dp)
                                            .align(Alignment.TopEnd)
                                            .clip(CircleShape)
                                            .background(Color(0xFFe94560)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (notifications.size > 9) "9+" else notifications.size.toString(),
                                            color = Color.White,
                                            fontSize = if (isTablet) 11.sp else 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(if (isTablet) 12.dp else 8.dp))

                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier
                                    .size(if (isTablet) 52.dp else 44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFe94560))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Cerrar sesión",
                                    tint = Color.White,
                                    modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                                )
                            }
                        }
                    }

                    // Banner de estado de conexión
                    ConnectionStatusBannerWaiter(
                        isInternetAvailable = isInternetAvailable,
                        isFirebaseConnected = isFirebaseConnected,
                        connectionStatusText = connectionStatusText,
                        onManualSync = { viewModel.syncWithFirebase() },
                        isTablet = isTablet
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (isTablet) 12.dp else 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SISTEMA MESERO",
                            style = if (isTablet) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 3.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (readyOrdersCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFe94560),
                                    modifier = Modifier.height(if (isTablet) 24.dp else 20.dp)
                                ) {
                                    Text(
                                        text = "$readyOrdersCount listas",
                                        color = Color.White,
                                        fontSize = if (isTablet) 11.sp else 9.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(if (isTablet) 10.dp else 8.dp))
                            }
                            Text(
                                text = "${tables.size} mesas | $occupiedTablesCount ocup.",
                                style = if (isTablet) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall,
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
                    onClearSuccess = { viewModel.clearSuccessMessage() },
                    isTablet = isTablet
                )

                // Panel de bienvenida - Responsivo y COMPACTO
                WelcomeCard(
                    connectionStatusText = connectionStatusText,
                    isInternetAvailable = isInternetAvailable,
                    isFirebaseConnected = isFirebaseConnected,
                    tablesCount = tables.size,
                    occupiedTablesCount = occupiedTablesCount,
                    readyOrdersCount = readyOrdersCount,
                    isTablet = isTablet,
                    accentColor = accentColor,
                    surfaceColor = surfaceColor
                )

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
                            .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 8.dp)
                    )
                }

                // Tabs principales
                Column(modifier = Modifier.fillMaxSize()) {
                    // Carrito flotante - Solo visible en móvil
                    if (totalItemsInCart > 0 && !isTablet) {
                        CartFloatingButton(
                            itemCount = totalItemsInCart,
                            onClick = {
                                viewModel.currentTableId.value?.let { tableId ->
                                    navController.navigate("table_details/$tableId")
                                }
                            }
                        )
                    }

                    // Tabs responsivos
                    if (isTablet && isLandscape) {
                        // En tablet landscape: mostrar tabs a la izquierda y contenido a la derecha
                        Row(modifier = Modifier.fillMaxSize()) {
                            NavigationRail(
                                modifier = Modifier.width(80.dp),
                                containerColor = surfaceColor.copy(alpha = 0.8f)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    NavigationRailItem(
                                        selected = selectedTab == 0,
                                        onClick = { selectedTab = 0; showNotifications = false },
                                        icon = { Icon(Icons.Default.TableRestaurant, contentDescription = "Mesas") },
                                        label = { Text("Mesas", fontSize = 10.sp) }
                                    )
                                    NavigationRailItem(
                                        selected = selectedTab == 1,
                                        onClick = { selectedTab = 1; showNotifications = false },
                                        icon = { Icon(Icons.Default.RestaurantMenu, contentDescription = "Productos") },
                                        label = { Text("Productos", fontSize = 10.sp) }
                                    )
                                    NavigationRailItem(
                                        selected = selectedTab == 2,
                                        onClick = { selectedTab = 2; showNotifications = false },
                                        icon = { Icon(Icons.Default.ListAlt, contentDescription = "Órdenes") },
                                        label = { Text("Órdenes", fontSize = 10.sp) }
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                            ) {
                                TabContent(
                                    selectedTab = selectedTab,
                                    navController = navController,
                                    viewModel = viewModel
                                )
                            }
                        }
                    } else {
                        // Modo normal: tabs en la parte superior
                        if (totalItemsInCart > 0 && isTablet) {
                            CartBar(itemCount = totalItemsInCart, isTablet = true)
                        }

                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = surfaceColor.copy(alpha = 0.6f),
                            contentColor = Color.White,
                            edgePadding = if (isTablet) 32.dp else 16.dp,
                            modifier = Modifier.padding(horizontal = if (isTablet) 24.dp else 16.dp)
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = {
                                    selectedTab = 0
                                    showNotifications = false
                                },
                                text = {
                                    Text(
                                        "MESAS",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = if (isTablet) 16.sp else 14.sp
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = {
                                    selectedTab = 1
                                    showNotifications = false
                                },
                                text = {
                                    Text(
                                        "PRODUCTOS",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = if (isTablet) 16.sp else 14.sp
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 2,
                                onClick = {
                                    selectedTab = 2
                                    showNotifications = false
                                },
                                text = {
                                    Text(
                                        "ÓRDENES",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = if (isTablet) 16.sp else 14.sp
                                    )
                                }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f)
                        ) {
                            TabContent(
                                selectedTab = selectedTab,
                                navController = navController,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabContent(
    selectedTab: Int,
    navController: NavController,
    viewModel: WaiterViewModel
) {
    when (selectedTab) {
        0 -> TablesScreen(navController = navController, viewModel = viewModel)
        1 -> ProductsScreen(navController = navController, viewModel = viewModel)
        2 -> OrdersScreen(navController = navController, viewModel = viewModel)
    }
}

@Composable
fun WelcomeCard(
    connectionStatusText: String,
    isInternetAvailable: Boolean,
    isFirebaseConnected: Boolean,
    tablesCount: Int,
    occupiedTablesCount: Int,
    readyOrdersCount: Int,
    isTablet: Boolean,
    accentColor: Color,
    surfaceColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 4.dp)  // ← Reducido vertical
            .shadow(if (isTablet) 4.dp else 2.dp, RoundedCornerShape(if (isTablet) 16.dp else 12.dp)),  // ← Sombra más pequeña
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor.copy(alpha = 0.9f)
        )
    ) {
        // Layout HORIZONTAL para ocupar menos espacio
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 16.dp else 12.dp),  // ← Padding reducido
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 8.dp)
        ) {
            // Icono más compacto
            Box(
                modifier = Modifier
                    .size(if (isTablet) 48.dp else 40.dp)  // ← Reducido
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
                    modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                )
            }

            // Contenido compacto en columna
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Fila 1: Saludo
                Text(
                    text = "¡Bienvenido, Mesero!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    maxLines = 1
                )

                // Fila 2: Toda la información en UNA SOLA LÍNEA (esto soluciona el problema)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Estado de conexión simplificado
                    Text(
                        text = when {
                            !isInternetAvailable -> "🔴 Offline"
                            isFirebaseConnected -> "🟢 Online"
                            else -> "🟡 Conectando..."
                        },
                        color = when {
                            !isInternetAvailable -> Color(0xFFF44336)
                            isFirebaseConnected -> Color(0xFF4CAF50)
                            else -> Color(0xFFFF9800)
                        },
                        fontSize = if (isTablet) 12.sp else 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )

                    Text(
                        text = "•",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = if (isTablet) 12.sp else 10.sp
                    )

                    Text(
                        text = "$tablesCount mesas",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = if (isTablet) 12.sp else 10.sp,
                        maxLines = 1
                    )

                    Text(
                        text = "•",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = if (isTablet) 12.sp else 10.sp
                    )

                    Text(
                        text = "$occupiedTablesCount ocup.",
                        color = if (occupiedTablesCount > 0) Color(0xFFFFA000) else Color.White.copy(alpha = 0.6f),
                        fontSize = if (isTablet) 12.sp else 10.sp,
                        fontWeight = if (occupiedTablesCount > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )

                    if (readyOrdersCount > 0) {
                        Text(
                            text = "•",
                            color = Color.White.copy(alpha = 0.3f),
                            fontSize = if (isTablet) 12.sp else 10.sp
                        )
                        Text(
                            text = "$readyOrdersCount listas",
                            color = Color(0xFFe94560),
                            fontSize = if (isTablet) 12.sp else 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartFloatingButton(
    itemCount: Int,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = Modifier
            .size(56.dp)
            .padding(4.dp),
        containerColor = Color(0xFFe94560)
    ) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = Color.White,
                    contentColor = Color(0xFFe94560)
                ) {
                    Text(itemCount.toString(), fontSize = 11.sp)
                }
            }
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito", tint = Color.White)
        }
    }
}

@Composable
fun CartBar(
    itemCount: Int,
    isTablet: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 16.dp else 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito", tint = Color.White, modifier = Modifier.size(if (isTablet) 24.dp else 20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$itemCount items en carrito",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = if (isTablet) 16.sp else 14.sp
            )
        }
    }
}

@Composable
fun ConnectionStatusBannerWaiter(
    isInternetAvailable: Boolean,
    isFirebaseConnected: Boolean,
    connectionStatusText: String,
    onManualSync: () -> Unit,
    isTablet: Boolean
) {
    val backgroundColor = when {
        !isInternetAvailable -> Color(0xFFF44336).copy(alpha = 0.9f)
        !isFirebaseConnected -> Color(0xFFFF9800).copy(alpha = 0.9f)
        else -> Color(0xFF4CAF50).copy(alpha = 0.9f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (isTablet) 12.dp else 8.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(if (isTablet) 12.dp else 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isTablet) 16.dp else 12.dp, vertical = if (isTablet) 12.dp else 8.dp),
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
                    modifier = Modifier.size(if (isTablet) 22.dp else 18.dp)
                )
                Spacer(modifier = Modifier.width(if (isTablet) 10.dp else 8.dp))
                Text(
                    text = connectionStatusText,
                    color = Color.White,
                    style = if (isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!isFirebaseConnected && isInternetAvailable) {
                TextButton(
                    onClick = onManualSync,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                    modifier = Modifier.height(if (isTablet) 36.dp else 28.dp)
                ) {
                    Text("Reconectar", fontSize = if (isTablet) 14.sp else 12.sp)
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
    onClearSuccess: () -> Unit,
    isTablet: Boolean
) {
    val paddingHorizontal = if (isTablet) 24.dp else 16.dp

    if (error != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = paddingHorizontal, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336).copy(alpha = 0.95f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isTablet) 16.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Error, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (isTablet) 24.dp else 20.dp))
                Spacer(modifier = Modifier.width(if (isTablet) 10.dp else 8.dp))
                Text(error, color = Color.White, modifier = Modifier.weight(1f), style = if (isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall)
                IconButton(onClick = onClearError, modifier = Modifier.size(if (isTablet) 32.dp else 28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(if (isTablet) 20.dp else 16.dp))
                }
            }
        }
    }

    if (warning != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = paddingHorizontal, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.95f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isTablet) 16.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (isTablet) 24.dp else 20.dp))
                Spacer(modifier = Modifier.width(if (isTablet) 10.dp else 8.dp))
                Text(warning, color = Color.White, modifier = Modifier.weight(1f), style = if (isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall)
                IconButton(onClick = onClearWarning, modifier = Modifier.size(if (isTablet) 32.dp else 28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(if (isTablet) 20.dp else 16.dp))
                }
            }
        }
    }

    if (success != null) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = paddingHorizontal, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.95f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isTablet) 16.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (isTablet) 24.dp else 20.dp))
                Spacer(modifier = Modifier.width(if (isTablet) 10.dp else 8.dp))
                Text(success, color = Color.White, modifier = Modifier.weight(1f), style = if (isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall)
                IconButton(onClick = onClearSuccess, modifier = Modifier.size(if (isTablet) 32.dp else 28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(if (isTablet) 20.dp else 16.dp))
                }
            }
        }
    }
}

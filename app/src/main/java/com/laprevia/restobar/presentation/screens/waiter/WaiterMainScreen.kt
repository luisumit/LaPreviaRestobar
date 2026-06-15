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
import com.laprevia.restobar.presentation.theme.SuccessGreen
import com.laprevia.restobar.presentation.theme.WarningOrange
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

    // Detectar tamano de pantalla para diseno responsivo
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

    // Auto-clear mensajes despues de 3 segundos
    LaunchedEffect(successMessage, errorMessage, connectionMessage) {
        if (successMessage != null || errorMessage != null || connectionMessage != null) {
            delay(3000)
            if (successMessage != null) viewModel.clearSuccessMessage()
            if (errorMessage != null) viewModel.clearError()
            if (connectionMessage != null) viewModel.clearConnectionMessage()
        }
    }

    val readyOrdersCount = orders.count { it.status == com.laprevia.restobar.data.model.OrderStatus.LISTO }
    val occupiedTablesCount = tables.count { it.status == com.laprevia.restobar.data.model.TableStatus.OCUPADA }
    val totalItemsInCart = currentOrderItems.sumOf { it.quantity }

    // Texto de estado de conexion mejorado con iconos SVG (sin emojis)
    val connectionStatusText = when {
        !isInternetAvailable -> "SIN INTERNET - Modo offline"
        isFirebaseConnected -> "Conectado a cocina"
        else -> "Conectando..."
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
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
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
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Black,
                                            fontSize = if (isTablet) 32.sp else 24.sp,
                                            letterSpacing = 2.sp
                                        )
                                    ) {
                                        append("LA PREVIA\n")
                                    }
                                    withStyle(
                                        SpanStyle(
                                            color = MaterialTheme.colorScheme.secondary,
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
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notificaciones",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                                    )
                                }

                                if (notifications.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(if (isTablet) 22.dp else 18.dp)
                                            .align(Alignment.TopEnd)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (notifications.size > 9) "9+" else notifications.size.toString(),
                                            color = MaterialTheme.colorScheme.onSecondary,
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
                                    .background(MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Cerrar sesion",
                                    tint = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                                )
                            }
                        }
                    }

                    // Banner de estado de conexion
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
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 3.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (readyOrdersCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.height(if (isTablet) 24.dp else 20.dp)
                                ) {
                                    Text(
                                        text = "$readyOrdersCount listas",
                                        color = MaterialTheme.colorScheme.onSecondary,
                                        fontSize = if (isTablet) 11.sp else 9.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(if (isTablet) 10.dp else 8.dp))
                            }
                            Text(
                                text = "${tables.size} mesas | $occupiedTablesCount ocup.",
                                style = if (isTablet) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                    accentColor = MaterialTheme.colorScheme.secondary,
                    surfaceColor = MaterialTheme.colorScheme.surface
                )

                // Contenido principal
                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (isTablet && isLandscape) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            NavigationRail(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(80.dp),
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                NavigationRailItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0; showNotifications = false },
                                    icon = { Icon(Icons.Default.TableBar, contentDescription = "Mesas") },
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
                                    icon = { Icon(Icons.Default.ListAlt, contentDescription = "Ordenes") },
                                    label = { Text("Ordenes", fontSize = 10.sp) }
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
                    } else {
                        // Modo normal: tabs en la parte superior
                        if (totalItemsInCart > 0 && isTablet) {
                            CartBar(itemCount = totalItemsInCart, isTablet = true)
                        }

                        ScrollableTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onSurface,
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
                                        "ORDENES",
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
            .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = 4.dp)
            .shadow(if (isTablet) 4.dp else 2.dp, RoundedCornerShape(if (isTablet) 16.dp else 12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = surfaceColor.copy(alpha = 0.9f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 16.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isTablet) 48.dp else 40.dp)
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
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(if (isTablet) 24.dp else 20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Bienvenido, Mesero!",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isTablet) 16.sp else 14.sp,
                    maxLines = 1
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (isTablet) 12.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusColor = when {
                        !isInternetAvailable -> MaterialTheme.colorScheme.error
                        isFirebaseConnected -> SuccessGreen
                        else -> WarningOrange
                    }
                    val statusIcon = when {
                        !isInternetAvailable -> Icons.Default.WifiOff
                        isFirebaseConnected -> Icons.Default.Wifi
                        else -> Icons.Default.Sync
                    }
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(if (isTablet) 14.dp else 12.dp)
                    )
                    Text(
                        text = when {
                            !isInternetAvailable -> "Offline"
                            isFirebaseConnected -> "Online"
                            else -> "Conectando..."
                        },
                        color = statusColor,
                        fontSize = if (isTablet) 12.sp else 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )

                    Text(
                        text = "\u2022",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        fontSize = if (isTablet) 12.sp else 10.sp
                    )

                    Text(
                        text = "$tablesCount mesas",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        fontSize = if (isTablet) 12.sp else 10.sp,
                        maxLines = 1
                    )

                    Text(
                        text = "\u2022",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        fontSize = if (isTablet) 12.sp else 10.sp
                    )

                    Text(
                        text = "$occupiedTablesCount ocup.",
                        color = if (occupiedTablesCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        fontSize = if (isTablet) 12.sp else 10.sp,
                        fontWeight = if (occupiedTablesCount > 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )

                    if (readyOrdersCount > 0) {
                        Text(
                            text = "\u2022",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            fontSize = if (isTablet) 12.sp else 10.sp
                        )
                        Text(
                            text = "$readyOrdersCount listas",
                            color = MaterialTheme.colorScheme.secondary,
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
        containerColor = MaterialTheme.colorScheme.secondary
    ) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.onSecondary,
                    contentColor = MaterialTheme.colorScheme.secondary
                ) {
                    Text(itemCount.toString(), fontSize = 11.sp)
                }
            }
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito", tint = MaterialTheme.colorScheme.onSecondary)
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
            containerColor = SuccessGreen.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isTablet) 16.dp else 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(if (isTablet) 24.dp else 20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$itemCount items en carrito",
                color = MaterialTheme.colorScheme.onSurface,
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
        !isInternetAvailable -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
        !isFirebaseConnected -> WarningOrange.copy(alpha = 0.9f)
        else -> SuccessGreen.copy(alpha = 0.9f)
    }

    val statusIcon = when {
        !isInternetAvailable -> Icons.Default.WifiOff
        !isFirebaseConnected -> Icons.Default.Sync
        else -> Icons.Default.Wifi
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
                    imageVector = statusIcon,
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.95f))
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
            colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.95f))
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
            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.95f))
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

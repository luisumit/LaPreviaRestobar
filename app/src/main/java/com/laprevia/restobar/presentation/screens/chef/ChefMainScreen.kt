package com.laprevia.restobar.presentation.screens.chef

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.laprevia.restobar.presentation.viewmodel.LoginViewModel // ✅ AGREGAR LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefMainScreen(
    chefViewModel: ChefViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel(), // ✅ AGREGAR LoginViewModel
    inventoryViewModel: InventoryViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {} // ✅ NUEVO PARÁMETRO AGREGADO
) {
    // Estados
    var selectedTab by remember { mutableStateOf(0) }
    var showNotifications by remember { mutableStateOf(false) }

    // ✅ SOLUCIÓN CORRECTA: Solo usar isFirebaseConnected
    val notifications by remember { chefViewModel.notifications }.collectAsState()
    val orders by remember { chefViewModel.orders }.collectAsState()
    val successMessage by remember { chefViewModel.successMessage }.collectAsState()
    val errorMessage by remember { chefViewModel.errorMessage }.collectAsState()
    val isFirebaseConnected by remember { chefViewModel.isFirebaseConnected }.collectAsState()

    // ✅ SOLUCIÓN: Calcular connectionStatus localmente basado en isFirebaseConnected
    val connectionStatusText = if (isFirebaseConnected) "🟢 Conectado a Firebase" else "🔴 Sin conexión"

    // Calcular contadores directamente desde las órdenes
    val newOrdersCount = orders.count { it.status == com.laprevia.restobar.data.model.OrderStatus.ENVIADO }
    val activeOrdersCount = orders.size

    val backgroundColor = Color(0xFF0f3460)
    val surfaceColor = Color(0xFF1a1a2e)
    val accentColor = Color(0xFFe94560)

    // Cargar órdenes al iniciar
    LaunchedEffect(Unit) {
        chefViewModel.refreshOrders()
    }

    // Manejar mensajes de éxito/error
    LaunchedEffect(successMessage) {
        successMessage?.let {
            chefViewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            chefViewModel.clearError()
        }
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
                            // Botón de notificaciones
                            BadgedBox(
                                badge = {
                                    if (notifications.isNotEmpty()) {
                                        Badge {
                                            Text(text = notifications.size.toString())
                                        }
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

                            // ✅ BOTÓN DE LOGOUT CORREGIDO - AHORA SÍ CIERRA SESIÓN
                            IconButton(
                                onClick = {
                                    println("🔄 ChefScreen: Cerrando sesión...")
                                    loginViewModel.signOut() // ✅ LLAMAR AL VIEWMODEL CORRECTO
                                    onLogout() // ✅ LLAMAR LA NAVEGACIÓN
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

                        // ✅ AHORA USA connectionStatusText
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = connectionStatusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isFirebaseConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                                fontSize = 10.sp
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Contadores rápidos
                            if (newOrdersCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFe94560)
                                ) {
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
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Panel de bienvenida ACTUALIZADO
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

                            // ESTADÍSTICAS MEJORADAS - MÁS LIMPIO
                            Row(
                                modifier = Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Estado de conexión
                                Text(
                                    text = if (isFirebaseConnected) "🟢 Conectado" else "🔴 Sin conexión",
                                    color = if (isFirebaseConnected) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                // Separador
                                Text(
                                    text = "•",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp
                                )

                                // Órdenes activas
                                Text(
                                    text = "$activeOrdersCount activas",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 10.sp
                                )
                            }

                            // Línea adicional para nuevas órdenes (solo si hay)
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

                // Panel de notificaciones (condicional)
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
                            text = {
                                Text(text = "ÓRDENES")
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                showNotifications = false
                            },
                            text = {
                                Text(text = "INVENTARIO")
                            }
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
package com.laprevia.restobar.presentation.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laprevia.restobar.presentation.notifications.AdminStockScheduler
import com.laprevia.restobar.presentation.theme.SuccessGreen
import com.laprevia.restobar.presentation.theme.WarningOrange
import com.laprevia.restobar.presentation.viewmodel.AdminViewModel
import com.laprevia.restobar.presentation.viewmodel.LoginViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    viewModel: AdminViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState = viewModel.uiState.collectAsState().value
    val context = LocalContext.current

    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    LaunchedEffect(Unit) {
        AdminStockScheduler.schedulePeriodicCheck(context)
    }

    LaunchedEffect(uiState.success, uiState.warning, uiState.error) {
        if (uiState.success != null || uiState.warning != null || uiState.error != null) {
            delay(3000)
            if (uiState.success != null) viewModel.clearSuccess()
            if (uiState.warning != null) viewModel.clearWarning()
            if (uiState.error != null) viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = if (isTablet) 12.dp else 8.dp,
                        shape = RoundedCornerShape(bottomStart = if (isTablet) 32.dp else 24.dp, bottomEnd = if (isTablet) 32.dp else 24.dp)
                    ),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = if (isTablet) 16.dp else 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "LA PREVIA RESTOBAR",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Panel Administrativo",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        IconButton(
                            onClick = {
                                AdminStockScheduler.triggerImmediateCheck(context)
                            },
                            modifier = Modifier
                                .size(if (isTablet) 48.dp else 44.dp)
                                .clip(CircleShape)
                                .background(WarningOrange)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Probar notificacion",
                                tint = Color.White,
                                modifier = Modifier.size(if (isTablet) 22.dp else 20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(if (isTablet) 8.dp else 4.dp))

                        IconButton(
                            onClick = {
                                timber.log.Timber.d("AdminScreen: Cerrando sesion...")
                                loginViewModel.signOut()
                                onLogout()
                            },
                            modifier = Modifier
                                .size(if (isTablet) 48.dp else 44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Cerrar sesion",
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(if (isTablet) 22.dp else 20.dp)
                            )
                        }
                    }

                    ConnectionStatusBanner(
                        isOffline = uiState.isOffline,
                        pendingSyncCount = uiState.pendingSyncCount,
                        connectionStatusText = viewModel.connectionStatusText,
                        onManualSync = { viewModel.manualSync() }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showProductForm() },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.shadow(if (isTablet) 12.dp else 8.dp, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Producto")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            MessageBanner(
                error = uiState.error,
                warning = uiState.warning,
                success = uiState.success,
                onClearError = { viewModel.clearError() },
                onClearWarning = { viewModel.clearWarning() },
                onClearSuccess = { viewModel.clearSuccess() }
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isTablet) 24.dp else 16.dp)
                    .shadow(if (isTablet) 8.dp else 4.dp, RoundedCornerShape(if (isTablet) 24.dp else 16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isTablet) 24.dp else 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isTablet) 80.dp else 60.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondary,
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = "Admin",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(if (isTablet) 36.dp else 28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(if (isTablet) 20.dp else 16.dp))

                    Column {
                        Text(
                            "Panel de Administracion",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            style = if (isTablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleLarge
                        )
                        Text(
                            viewModel.connectionStatusText,
                            color = when {
                                uiState.isOffline -> MaterialTheme.colorScheme.error
                                uiState.pendingSyncCount > 0 -> WarningOrange
                                else -> SuccessGreen
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isTablet) 24.dp else 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProductStatCard(
                    title = "Total Productos",
                    value = uiState.products.size.toString(),
                    gradientColors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(if (isTablet) 16.dp else 12.dp))
                ProductStatCard(
                    title = "Activos",
                    value = uiState.products.count { it.isActive }.toString(),
                    gradientColors = listOf(SuccessGreen, MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isTablet) 24.dp else 16.dp)
                    .padding(top = if (isTablet) 16.dp else 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProductStatCard(
                    title = "Con Inventario",
                    value = uiState.products.count { it.trackInventory }.toString(),
                    gradientColors = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(if (isTablet) 16.dp else 12.dp))
                ProductStatCard(
                    title = "Categorias",
                    value = uiState.categories.size.toString(),
                    gradientColors = listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(if (isTablet) 24.dp else 20.dp))

            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = if (isTablet) 24.dp else 16.dp, vertical = if (isTablet) 12.dp else 8.dp)
                    .shadow(if (isTablet) 8.dp else 4.dp, RoundedCornerShape(if (isTablet) 24.dp else 16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isTablet) 24.dp else 16.dp)
                ) {
                    Text(
                        text = "Productos Registrados",
                        style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = if (isTablet) 20.dp else 16.dp)
                    )

                    if (uiState.products.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Inventory,
                                    contentDescription = "Sin productos",
                                    modifier = Modifier.size(if (isTablet) 80.dp else 64.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(if (isTablet) 20.dp else 16.dp))
                                Text(
                                    text = "No hay productos registrados",
                                    style = if (isTablet) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Presiona el boton + para agregar uno",
                                    style = if (isTablet) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(if (isTablet) 16.dp else 12.dp)
                        ) {
                            items(uiState.products) { product ->
                                ProductAdminCard(
                                    product = product,
                                    onEdit = { viewModel.showProductForm(product) },
                                    onDelete = { viewModel.showDeleteDialog(product) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showProductForm) {
        ProductFormDialog(
            product = uiState.selectedProduct,
            categories = uiState.categories,
            onSave = { product ->
                if (uiState.selectedProduct == null) {
                    viewModel.createProduct(product)
                } else {
                    viewModel.updateProduct(product)
                }
            },
            onDismiss = { viewModel.hideProductForm() }
        )
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideDeleteDialog() },
            title = { Text("Eliminar Producto") },
            text = {
                Text("Estas seguro de que quieres eliminar \"${uiState.selectedProduct?.name}\"?")
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteProduct() }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDeleteDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ConnectionStatusBanner(
    isOffline: Boolean,
    pendingSyncCount: Int,
    connectionStatusText: String,
    onManualSync: () -> Unit
) {
    val backgroundColor = when {
        isOffline -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
        pendingSyncCount > 0 -> WarningOrange.copy(alpha = 0.9f)
        else -> SuccessGreen.copy(alpha = 0.9f)
    }

    val statusIcon = when {
        isOffline -> Icons.Default.WifiOff
        pendingSyncCount > 0 -> Icons.Default.Sync
        else -> Icons.Default.Wifi
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
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
                    imageVector = statusIcon,
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

            if (pendingSyncCount > 0) {
                TextButton(
                    onClick = onManualSync,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("Sincronizar", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                }
            }
        }
    }
}

@Composable
fun MessageBanner(
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.95f))
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
            colors = CardDefaults.cardColors(containerColor = WarningOrange.copy(alpha = 0.95f))
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
            colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.95f))
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

@Composable
fun ProductStatCard(
    title: String,
    value: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Card(
        modifier = modifier
            .height(if (isTablet) 120.dp else 100.dp)
            .shadow(if (isTablet) 12.dp else 8.dp, RoundedCornerShape(if (isTablet) 20.dp else 16.dp)),
        shape = RoundedCornerShape(if (isTablet) 20.dp else 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(gradientColors),
                    shape = RoundedCornerShape(if (isTablet) 20.dp else 16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isTablet) 20.dp else 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = value,
                    style = if (isTablet) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = title,
                    style = if (isTablet) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

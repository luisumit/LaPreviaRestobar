package com.laprevia.restobar.presentation.screens.waiter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.presentation.viewmodel.WaiterViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    navController: NavController,
    viewModel: WaiterViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    val isInternetAvailable by viewModel.isInternetAvailable.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()
    val connectionMessage by viewModel.connectionMessage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Variable local para evitar smart cast issues
    val currentConnectionMessage = connectionMessage

    // Auto-clear mensaje de conexión
    LaunchedEffect(currentConnectionMessage) {
        if (currentConnectionMessage != null && currentConnectionMessage.contains("SIN INTERNET")) {
            delay(3000)
            viewModel.clearConnectionMessage()
        }
    }

    val inventoryProducts = products.filter { it.trackInventory }

    // Texto de estado de conexión
    val connectionStatusText = when {
        !isInternetAvailable -> "🔴 SIN INTERNET - Inventario local"
        isFirebaseConnected -> "🟢 Conectado"
        else -> "🟡 Reconectando..."
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Control de Inventario") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Indicador de conexión en la barra superior
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (!isInternetAvailable) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Sin conexión",
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(18.dp)
                            )
                        } else if (!isFirebaseConnected) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Reconectando",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Banner de conexión
            if (!isInternetAvailable) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336).copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = "Sin conexión",
                            tint = Color(0xFFF44336)
                        )
                        Column {
                            Text(
                                text = connectionStatusText,
                                color = Color(0xFFF44336),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Mostrando inventario local. Conéctate para sincronizar.",
                                color = Color(0xFFF44336),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Mensaje temporal de conexión
            if (currentConnectionMessage != null && currentConnectionMessage.contains("SIN INTERNET")) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.9f))
                ) {
                    Text(
                        text = currentConnectionMessage,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            InventorySummary(
                totalProducts = inventoryProducts.size,
                lowStockProducts = inventoryProducts.count {
                    it.stock <= it.minStock && it.stock > 0
                },
                outOfStockProducts = inventoryProducts.count { it.stock == 0.0 },
                isOffline = !isInternetAvailable,
                modifier = Modifier.padding(16.dp)
            )

            if (isLoading && inventoryProducts.isEmpty()) {
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
                            text = if (!isInternetAvailable) "Cargando inventario local..." else "Cargando inventario...",
                            color = Color.White
                        )
                    }
                }
            } else if (inventoryProducts.isEmpty()) {
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
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Sin inventario",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (!isInternetAvailable) "Sin conexión a internet" else "No hay productos en inventario",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = if (!isInternetAvailable)
                                "El inventario se sincronizará cuando vuelva internet"
                            else
                                "Agrega productos con control de inventario desde el panel de administración",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.4f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (!isInternetAvailable) {
                            Button(
                                onClick = { viewModel.syncWithFirebase() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFe94560))
                            ) {
                                Text("Reintentar Conexión")
                            }
                        }
                    }
                }
            } else {
                if (!isInternetAvailable) {
                    Text(
                        text = "📱 Modo offline - Mostrando inventario local",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(inventoryProducts) { product ->
                        InventoryProductCard(
                            product = product,
                            isOffline = !isInternetAvailable
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Agregar Producto") },
            text = { Text("Para agregar productos al inventario, ve a la sección de Administración.") },
            confirmButton = {
                Button(onClick = { showAddDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
fun InventorySummary(
    totalProducts: Int,
    lowStockProducts: Int,
    outOfStockProducts: Int,
    isOffline: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a2e).copy(alpha = 0.8f)
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
                    text = "Resumen de Inventario",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (isOffline) {
                    Text(
                        text = "📱 Modo local",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFF9800)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                InventoryStatItem(
                    count = totalProducts,
                    label = "Total",
                    color = Color(0xFF2196F3)
                )
                InventoryStatItem(
                    count = lowStockProducts,
                    label = "Stock Bajo",
                    color = Color(0xFFFFA000)
                )
                InventoryStatItem(
                    count = outOfStockProducts,
                    label = "Agotados",
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
fun InventoryStatItem(
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
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun InventoryProductCard(
    product: Product,
    isOffline: Boolean = false
) {
    val currentStock = product.stock
    val minStock = product.minStock
    val stockColor = when {
        currentStock == 0.0 -> Color(0xFFF44336)
        currentStock <= minStock -> Color(0xFFFFA000)
        else -> Color(0xFF4CAF50)
    }

    val stockStatus = when {
        currentStock == 0.0 -> "AGOTADO"
        currentStock <= minStock -> "STOCK BAJO"
        else -> "DISPONIBLE"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a2e).copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = "Categoría: ${product.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Precio: S/. ${product.salePrice ?: 0.0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "$currentStock unidades",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = stockColor
                    )
                    Text(
                        text = stockStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = stockColor
                    )
                }
            }

            if (product.trackInventory) {
                Spacer(modifier = Modifier.height(8.dp))
                val maxStockForVisualization = 50.0
                val progress = (currentStock / maxStockForVisualization).toFloat().coerceIn(0.0f, 1.0f)

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = stockColor,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )

                if (minStock > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Stock mínimo: $minStock unidades",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (product.isActive) "🟢 Activo" else "🔴 Inactivo",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (product.isActive) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Text(
                    text = if (product.trackInventory) "📦 Controla inventario" else "📋 Sin control",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            if (isOffline) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Modo local",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF9800).copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ProductsScreen.kt - VERSIÓN ACTUALIZADA CON MONITOREO DE CONEXIÓN
package com.laprevia.restobar.presentation.screens.waiter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import androidx.navigation.NavController
import com.laprevia.restobar.presentation.screens.waiter.components.ProductItem
import com.laprevia.restobar.presentation.viewmodel.WaiterViewModel

@Composable
fun ProductsScreen(
    navController: NavController,
    viewModel: WaiterViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    val currentOrderItems by viewModel.currentOrderItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentTableId by viewModel.currentTableId.collectAsState()
    val isInternetAvailable by viewModel.isInternetAvailable.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()
    val connectionMessage by viewModel.connectionMessage.collectAsState()

    // Variable local para evitar smart cast issues
    val currentConnectionMessage = connectionMessage

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    // Texto de estado de conexión
    val connectionStatusText = when {
        !isInternetAvailable -> "🔴 SIN INTERNET - Productos locales disponibles"
        !isFirebaseConnected -> "🟡 Reconectando con el servidor..."
        else -> "🟢 Conectado"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Banner de estado de conexión
        if (!isInternetAvailable) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF44336).copy(alpha = 0.15f)
                )
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
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = connectionStatusText,
                        color = Color(0xFFF44336),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { viewModel.syncWithFirebase() },
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800)
                        )
                    ) {
                        Text("Reconectar", fontSize = MaterialTheme.typography.labelSmall.fontSize)
                    }
                }
            }
        }

        // Mensaje temporal de conexión
        if (currentConnectionMessage != null && currentConnectionMessage.contains("SIN INTERNET")) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
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

        // Información del pedido actual
        if (currentOrderItems.isNotEmpty()) {
            CurrentOrderMiniSummary(
                itemCount = currentOrderItems.sumOf { it.quantity },
                total = viewModel.currentOrderTotal,
                onViewOrder = {
                    currentTableId?.let { tableId ->
                        navController.navigate("table_details/$tableId")
                    }
                },
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (isLoading && products.isEmpty()) {
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
                        if (!isInternetAvailable) "Sin conexión - Mostrando productos locales..." else "Cargando productos...",
                        color = Color.White
                    )
                }
            }
        } else if (products.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (!isInternetAvailable) "Sin conexión a internet" else "No hay productos disponibles",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = if (!isInternetAvailable)
                            "Los productos se sincronizarán cuando vuelva internet"
                        else
                            "Los productos aparecerán aquí cuando estén configurados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    if (!isInternetAvailable) {
                        Button(
                            onClick = { viewModel.syncWithFirebase() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFe94560)),
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        } else {
            // Filtrar solo productos activos
            val activeProducts = products.filter { it.isActive }

            if (activeProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "No hay productos activos",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Todos los productos están inactivos",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.4f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Mostrar mensaje de modo offline si corresponde
                if (!isInternetAvailable) {
                    Text(
                        text = "📱 Modo offline - Mostrando productos guardados localmente",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeProducts) { product ->
                        ProductItem(
                            product = product,
                            onAddToOrder = {
                                if (currentTableId == null) {
                                    navController.navigate("tables") {
                                        popUpTo("products") { saveState = true }
                                    }
                                } else {
                                    viewModel.addItemToCurrentOrder(product)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentOrderMiniSummary(
    itemCount: Int,
    total: Double,
    onViewOrder: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a2e).copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Pedido actual",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = "$itemCount items • Total: S/. ${"%.2f".format(total)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }

            TextButton(onClick = onViewOrder) {
                Text(
                    text = "Ver pedido",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFe94560),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
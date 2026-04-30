package com.laprevia.restobar.presentation.screens.waiter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.laprevia.restobar.data.model.TableStatus
import com.laprevia.restobar.presentation.screens.waiter.components.ProductItem
import com.laprevia.restobar.presentation.viewmodel.WaiterViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TableDetailsScreen(
    navController: NavController,
    tableId: String?,
    viewModel: WaiterViewModel = hiltViewModel()
) {
    val tables by viewModel.tables.collectAsState()
    val products by viewModel.products.collectAsState()
    val currentOrderItems by viewModel.currentOrderItems.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val connectionMessage by viewModel.connectionMessage.collectAsState()
    val isInternetAvailable by viewModel.isInternetAvailable.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()

    val table = tables.find { it.id == tableId?.toIntOrNull() }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // Variable local para evitar smart cast issues
    val currentConnectionMessage = connectionMessage

    // Auto-clear mensaje de conexión
    LaunchedEffect(currentConnectionMessage) {
        if (currentConnectionMessage != null) {
            delay(3000)
            viewModel.clearConnectionMessage()
        }
    }

    LaunchedEffect(tableId) {
        tableId?.toIntOrNull()?.let { id ->
            viewModel.setCurrentTable(id)
        }
    }

    // Mostrar mensaje y navegar de regreso
    LaunchedEffect(successMessage) {
        if (successMessage?.contains("Pedido") == true || successMessage?.contains("guardado") == true) {
            delay(2000)
            navController.popBackStack()
            viewModel.clearSuccessMessage()
        }
    }

    if (table == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Mesa no encontrada")
        }
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column {
                        Text("Mesa ${table.number}")
                        Text(
                            text = when {
                                !isInternetAvailable -> "🔴 Sin conexión"
                                table.status == TableStatus.LIBRE -> "Libre"
                                else -> "Ocupada"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (!isInternetAvailable) Color(0xFFF44336) else Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.clearCurrentOrder()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentOrderItems.isNotEmpty()) {
                        BadgedBox(
                            badge = {
                                Badge {
                                    Text(currentOrderItems.sumOf { it.quantity }.toString())
                                }
                            }
                        ) {
                            IconButton(onClick = { showConfirmDialog = true }) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito")
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Banner de estado de conexión (sin internet)
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
                            Icons.Default.Warning,
                            contentDescription = "Sin internet",
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "📱 SIN INTERNET - Los pedidos se guardarán localmente y se enviarán cuando vuelva la conexión",
                            color = Color(0xFFF44336),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Mensaje temporal de conexión
            if (currentConnectionMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentConnectionMessage.contains("SIN INTERNET"))
                            Color(0xFFFF9800).copy(alpha = 0.9f)
                        else
                            Color(0xFF4CAF50).copy(alpha = 0.9f)
                    )
                ) {
                    Text(
                        text = currentConnectionMessage,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (currentOrderItems.isNotEmpty()) {
                CurrentOrderSummary(
                    cartItems = currentOrderItems,
                    onClearCart = { viewModel.clearCurrentOrder() },
                    onSendOrder = { showConfirmDialog = true },
                    viewModel = viewModel,
                    isOffline = !isInternetAvailable
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(products.filter { it.isActive }) { product ->
                    ProductItem(
                        product = product,
                        onAddToOrder = {
                            viewModel.addItemToCurrentOrder(product)
                        }
                    )
                }
            }
        }
    }

    if (showConfirmDialog) {
        // ✅ CORREGIDO: Asegurar que se pasa table.id (no table.number)
        ConfirmOrderDialog(
            cartItems = currentOrderItems,
            tableNumber = table.number,
            isOffline = !isInternetAvailable,
            onConfirm = {
                // ✅ IMPORTANTE: Usar table.id (no table.number)
                println("📤 Enviando pedido - tableId: ${table.id}, tableNumber: ${table.number}")
                viewModel.createOrder(
                    tableId = table.id,      // ✅ Esto debe ser 1,2,3,4,5,6,7,8
                    tableNumber = table.number
                )
                showConfirmDialog = false
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}

@Composable
fun CurrentOrderSummary(
    cartItems: List<com.laprevia.restobar.data.model.OrderItem>,
    onClearCart: () -> Unit,
    onSendOrder: () -> Unit,
    viewModel: WaiterViewModel,
    isOffline: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    "Pedido Actual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearCart) {
                    Text("Limpiar")
                }
            }

            if (isOffline) {
                Text(
                    "📱 Modo offline - El pedido se guardará localmente",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            cartItems.forEach { item ->
                OrderItemRow(
                    item = item,
                    viewModel = viewModel
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Total:",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "S/. ${"%.2f".format(cartItems.sumOf { it.subtotal })}",
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onSendOrder,
                modifier = Modifier.fillMaxWidth(),
                enabled = cartItems.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOffline) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isOffline) "Enviar Pedido (Modo offline)" else "Enviar Pedido a Cocina")
            }

            if (isOffline) {
                Text(
                    text = "⚠️ Sin internet. El pedido se guardará y se enviará automáticamente cuando vuelva la conexión.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
fun OrderItemRow(
    item: com.laprevia.restobar.data.model.OrderItem,
    viewModel: WaiterViewModel
) {
    var showEditDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${item.quantity}x ${item.productName}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (item.productDescription.isNotBlank()) {
                Text(
                    text = item.productDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "S/. ${"%.2f".format(item.subtotal)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            TextButton(onClick = { showEditDialog = true }) {
                Text("Editar")
            }
        }
    }

    if (showEditDialog) {
        EditQuantityDialog(
            currentQuantity = item.quantity,
            productName = item.productName,
            onConfirm = { newQuantity ->
                viewModel.updateItemQuantity(item.productId, newQuantity)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }
}

@Composable
fun EditQuantityDialog(
    currentQuantity: Int,
    productName: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var quantity by remember { mutableStateOf(currentQuantity.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Cantidad - $productName") },
        text = {
            Column {
                Text("Cantidad actual: $currentQuantity")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { newValue ->
                        quantity = newValue.filter { it.isDigit() }
                    },
                    label = { Text("Nueva cantidad") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newQty = quantity.toIntOrNull() ?: currentQuantity
                    if (newQty > 0) {
                        onConfirm(newQty)
                    }
                }
            ) {
                Text("Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ConfirmOrderDialog(
    cartItems: List<com.laprevia.restobar.data.model.OrderItem>,
    tableNumber: Int,
    isOffline: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Pedido - Mesa $tableNumber") },
        text = {
            Column {
                if (isOffline) {
                    Text(
                        "📱 SIN INTERNET - El pedido se guardará localmente y se enviará cuando vuelva la conexión",
                        color = Color(0xFFFF9800),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text("¿Estás seguro de enviar este pedido a cocina?")
                Spacer(modifier = Modifier.height(16.dp))

                cartItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${item.quantity}x ${item.productName}")
                        Text("S/. ${"%.2f".format(item.subtotal)}")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total:", fontWeight = FontWeight.Bold)
                    Text(
                        "S/. ${"%.2f".format(cartItems.sumOf { it.subtotal })}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOffline) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isOffline) "Guardar Pedido Localmente" else "Enviar a Cocina")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancelar")
            }
        }
    )
}
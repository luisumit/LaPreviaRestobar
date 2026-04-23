package com.laprevia.restobar.presentation.screens.waiter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.laprevia.restobar.data.model.TableStatus
import com.laprevia.restobar.presentation.screens.waiter.components.ProductItem
import com.laprevia.restobar.presentation.viewmodel.WaiterViewModel

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

    val table = tables.find { it.id == tableId?.toIntOrNull() }
    var showConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(tableId) {
        tableId?.toIntOrNull()?.let { id ->
            viewModel.setCurrentTable(id)
        }
    }

    // Mostrar mensaje de éxito y navegar de regreso
    LaunchedEffect(successMessage) {
        if (successMessage?.contains("Pedido enviado") == true) {
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
                            "Estado: ${if (table.status == TableStatus.LIBRE) "Libre" else "Ocupada"}",
                            style = MaterialTheme.typography.bodySmall
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
            if (currentOrderItems.isNotEmpty()) {
                CurrentOrderSummary(
                    cartItems = currentOrderItems,
                    onClearCart = { viewModel.clearCurrentOrder() },
                    onSendOrder = { showConfirmDialog = true },
                    viewModel = viewModel
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
        ConfirmOrderDialog(
            cartItems = currentOrderItems,
            tableNumber = table.number,
            onConfirm = {
                viewModel.createOrder(
                    tableId = table.id,
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
    viewModel: WaiterViewModel
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
                enabled = cartItems.isNotEmpty()
            ) {
                Text("Enviar Pedido a Cocina")
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
            // ✅ ACTUALIZADO: Usar productName en lugar de product.name
            Text(
                text = "${item.quantity}x ${item.productName}",
                style = MaterialTheme.typography.bodyMedium
            )
            // ✅ ACTUALIZADO: Usar productDescription en lugar de product.description
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
            // ✅ ACTUALIZADO: Usar productName en lugar de product.name
            productName = item.productName,
            onConfirm = { newQuantity ->
                // ✅ ACTUALIZADO: Usar productId en lugar de product.id
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
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirmar Pedido - Mesa $tableNumber") },
        text = {
            Column {
                Text("¿Estás seguro de enviar este pedido a cocina?")
                Spacer(modifier = Modifier.height(16.dp))

                cartItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // ✅ ACTUALIZADO: Usar productName en lugar de product.name
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
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Enviar a Cocina")
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
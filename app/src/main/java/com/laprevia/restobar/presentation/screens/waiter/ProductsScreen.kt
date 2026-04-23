// ProductsScreen.kt - VERSIÓN CORREGIDA
package com.laprevia.restobar.presentation.screens.waiter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Información del pedido actual
        if (currentOrderItems.isNotEmpty()) {
            CurrentOrderMiniSummary(
                itemCount = currentOrderItems.sumOf { it.quantity },
                total = viewModel.currentOrderTotal,
                onViewOrder = {
                    // Navegar a la mesa actual si hay una seleccionada
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
                        "Cargando productos...",
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
                        text = "No hay productos disponibles",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Los productos aparecerán aquí cuando estén configurados",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.4f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // Filtrar solo productos activos (eliminamos isSellable ya que no existe)
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeProducts) { product ->
                        ProductItem(
                            product = product,
                            onAddToOrder = {
                                viewModel.addItemToCurrentOrder(product)
                                // Si no hay mesa seleccionada, navegar a mesas
                                if (currentTableId == null) {
                                    navController.navigate("tables") {
                                        popUpTo("products") { saveState = true }
                                    }
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
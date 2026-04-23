package com.laprevia.restobar.presentation.screens.waiter

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.presentation.viewmodel.WaiterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    navController: NavController,
    viewModel: WaiterViewModel = hiltViewModel()
) {
    val products by viewModel.products.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    val inventoryProducts = products.filter { it.trackInventory }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Control de Inventario") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            InventorySummary(
                totalProducts = inventoryProducts.size,
                lowStockProducts = inventoryProducts.count {
                    it.stock <= it.minStock && it.stock > 0
                },
                outOfStockProducts = inventoryProducts.count {
                    it.stock == 0.0
                },
                modifier = Modifier.padding(16.dp)
            )

            if (inventoryProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Sin inventario",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No hay productos en inventario",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Agrega productos con control de inventario",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    items(inventoryProducts) { product ->
                        InventoryProductCard(
                            product = product
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        // Este diálogo debería redirigir a la pantalla de administración
        // o usar el AdminViewModel para crear productos
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
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Resumen de Inventario",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                InventoryStatItem(
                    count = totalProducts,
                    label = "Total",
                    color = MaterialTheme.colorScheme.primary
                )
                InventoryStatItem(
                    count = lowStockProducts,
                    label = "Stock Bajo",
                    color = Color(0xFFFFA000)
                )
                InventoryStatItem(
                    count = outOfStockProducts,
                    label = "Agotados",
                    color = MaterialTheme.colorScheme.error
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
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun InventoryProductCard(
    product: Product
) {
    val currentStock = product.stock
    val minStock = product.minStock
    val stockColor = when {
        currentStock == 0.0 -> MaterialTheme.colorScheme.error
        currentStock <= minStock -> Color(0xFFFFA000)
        else -> MaterialTheme.colorScheme.primary
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
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Categoría: ${product.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Precio: S/. ${product.salePrice ?: 0.0}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                // Calculamos el progreso basado en stock máximo (asumimos 50 como máximo para visualización)
                val maxStockForVisualization = 50.0
                val progress = (currentStock / maxStockForVisualization).toFloat().coerceIn(0.0f, 1.0f)

                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = stockColor,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                // Mostrar stock mínimo si está configurado
                if (minStock > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Stock mínimo: $minStock unidades",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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
                    color = if (product.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                // Eliminamos la línea de "isSellable" ya que no existe en tu modelo
                // En su lugar, mostramos si controla inventario
                Text(
                    text = if (product.trackInventory) "📦 Controla inventario" else "📋 Sin control",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (product.trackInventory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
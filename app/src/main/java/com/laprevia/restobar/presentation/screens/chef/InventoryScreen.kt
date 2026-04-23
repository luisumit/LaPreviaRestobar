// app/src/main/java/com/laprevia/restobar/presentation/screens/chef/InventoryScreen.kt
package com.laprevia.restobar.presentation.screens.chef

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.laprevia.restobar.presentation.theme.GreenSuccess
import com.laprevia.restobar.presentation.theme.OrangeWarning
import com.laprevia.restobar.presentation.theme.RedError
import com.laprevia.restobar.presentation.viewmodel.InventoryViewModel
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign

@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val inventory by viewModel.inventory.collectAsState()
    val lowStockItems by viewModel.lowStockItems.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Logs de diagnóstico
    println("📱 InventoryScreen: Composición - Inventario: ${inventory.size} items")
    println("📱 InventoryScreen: Cargando: $isLoading")
    println("📱 InventoryScreen: Stock bajo: ${lowStockItems.size} items")
    println("📱 InventoryScreen: Categoría seleccionada: $selectedCategory")

    LaunchedEffect(Unit) {
        println("📱 InventoryScreen: LaunchedEffect ejecutado - ViewModel iniciado")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0f3460))
    ) {
        // Header con opción de inicializar datos
        InventoryHeader(
            totalItems = inventory.size,
            lowStockCount = lowStockItems.size,
            isLoading = isLoading,
            onInitializeData = { viewModel.initializeSampleData() },
            onRefresh = { viewModel.refreshInventory() },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Solo mostrar filtros si hay datos
        if (inventory.isNotEmpty()) {
            CategoryFilter(
                categories = viewModel.getCategories(),
                selectedCategory = selectedCategory,
                onCategorySelected = viewModel::filterByCategory,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        // Contenido principal
        when {
            isLoading -> {
                InventoryLoadingState()
            }
            inventory.isEmpty() -> {
                InventoryEmptyState(
                    onRefresh = { viewModel.refreshInventory() },
                    onInitializeData = { viewModel.initializeSampleData() }
                )
            }
            else -> {
                InventoryList(
                    inventory = inventory,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun InventoryHeader(
    totalItems: Int,
    lowStockCount: Int,
    isLoading: Boolean,
    onInitializeData: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a2e).copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isLoading) "Cargando..." else "Total Productos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = if (isLoading) "..." else totalItems.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Botón de refresh
                Button(
                    onClick = onRefresh,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16213e)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refrescar",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Actualizar")
                }

                if (lowStockCount > 0) {
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Stock bajo",
                                tint = RedError,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Stock Bajo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = RedError
                            )
                        }
                        Text(
                            text = "$lowStockCount productos",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = RedError
                        )
                    }
                }
            }

            // Botón para inicializar datos si no hay items
            if (totalItems == 0 && !isLoading) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onInitializeData,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFe94560)
                    )
                ) {
                    Text("Cargar Datos de Ejemplo")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFilter(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Categorías",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = {
                    Text(
                        "Todos",
                        color = if (selectedCategory == null) Color.White else Color.White.copy(alpha = 0.8f)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFe94560),
                    containerColor = Color(0xFF16213e),
                    selectedLabelColor = Color.White,
                    labelColor = Color.White.copy(alpha = 0.8f)
                ),
                modifier = Modifier.height(32.dp)
            )

            categories.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = {
                        Text(
                            category,
                            color = if (selectedCategory == category) Color.White else Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFe94560),
                        containerColor = Color(0xFF16213e),
                        selectedLabelColor = Color.White,
                        labelColor = Color.White.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.height(32.dp)
                )
            }
        }
    }
}

@Composable
fun InventoryList(
    inventory: List<com.laprevia.restobar.data.model.Inventory>,
    modifier: Modifier = Modifier
) {
    if (inventory.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay productos que coincidan con el filtro",
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            )
        ) {
            items(inventory) { inventoryItem ->
                InventoryItem(inventory = inventoryItem)
            }
        }
    }
}

@Composable
fun InventoryItem(inventory: com.laprevia.restobar.data.model.Inventory) {
    val maxStock = inventory.minimumStock * 3
    val progress = if (maxStock > 0) {
        (inventory.currentStock / maxStock).coerceIn(0.0, 1.0)
    } else {
        0.0
    }

    val progressColor = when {
        inventory.currentStock == 0.0 -> RedError
        inventory.currentStock <= inventory.minimumStock -> RedError
        inventory.currentStock <= inventory.minimumStock * 1.5 -> OrangeWarning
        else -> GreenSuccess
    }

    val stockStatus = when {
        inventory.currentStock == 0.0 -> "AGOTADO"
        inventory.currentStock <= inventory.minimumStock -> "STOCK BAJO"
        inventory.currentStock <= inventory.minimumStock * 1.5 -> "STOCK MEDIO"
        else -> "STOCK SUFICIENTE"
    }

    val stockColor = when {
        inventory.currentStock == 0.0 -> RedError
        inventory.currentStock <= inventory.minimumStock -> RedError
        inventory.currentStock <= inventory.minimumStock * 1.5 -> OrangeWarning
        else -> GreenSuccess
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a2e).copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // PRIMERA LÍNEA: Nombre del producto y stock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Nombre del producto - MÁS ESPACIO
                Text(
                    text = inventory.productName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f) // Toma más espacio
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Stock actual - COMPACTO
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "${inventory.currentStock.toInt()} ${inventory.unitOfMeasure}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = stockColor
                    )

                    Text(
                        text = stockStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = progressColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // SEGUNDA LÍNEA: Información adicional
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mínimo: ${inventory.minimumStock.toInt()} ${inventory.unitOfMeasure}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )

                    inventory.category?.let { category ->
                        Text(
                            text = "Categoría: $category",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // TERCERA LÍNEA: Barra de progreso
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            ) {
                // Fondo de la barra
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                )
                // Progreso actual
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.toFloat())
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(progressColor)
                )
            }

            // Cuarta línea: Indicador de niveles
            if (inventory.minimumStock > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Mín: ${inventory.minimumStock.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = OrangeWarning
                    )
                    Text(
                        text = "${maxStock.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
fun InventoryLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFFe94560))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Cargando inventario...",
                color = Color.White
            )
            Text(
                text = "Conectando con Firebase",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun InventoryEmptyState(
    onRefresh: () -> Unit,
    onInitializeData: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Sin datos",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No hay productos en inventario",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Puedes cargar datos de ejemplo para probar la funcionalidad",
                color = Color.White.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onInitializeData,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFe94560)
                )
            ) {
                Text("Cargar Datos de Ejemplo")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF16213e)
                )
            ) {
                Text("Reintentar Carga")
            }
        }
    }
}
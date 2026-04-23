// TablesScreen.kt - VERSIÓN CORREGIDA
package com.laprevia.restobar.presentation.screens.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.laprevia.restobar.presentation.screens.waiter.components.TableCard
import com.laprevia.restobar.presentation.viewmodel.WaiterViewModel

@Composable
fun TablesScreen(
    navController: NavController,
    viewModel: WaiterViewModel = hiltViewModel()
) {
    val tables by viewModel.tables.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // Fondo general para toda la pantalla
    ) {
        if (isLoading && tables.isEmpty()) {
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
                        "Cargando mesas...",
                        color = Color.White
                    )
                }
            }
        } else if (tables.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "No hay mesas disponibles",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Las mesas aparecerán aquí cuando estén configuradas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.4f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            // Resumen rápido de mesas - AHORA DENTRO DEL COLUMN PRINCIPAL
            TablesSummary(
                totalTables = tables.size,
                occupiedTables = tables.count { it.status == com.laprevia.restobar.data.model.TableStatus.OCUPADA },
                freeTables = tables.count { it.status == com.laprevia.restobar.data.model.TableStatus.LIBRE },
                totalOrders = orders.size,
                modifier = Modifier.padding(16.dp)
            )

            // Grid de mesas - OCUPA EL ESPACIO RESTANTE
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // ✅ IMPORTANTE: Esto hace que ocupe el espacio restante
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp) // ✅ Sin padding top para evitar superposición
            ) {
                items(tables.sortedBy { it.number }) { table ->
                    TableCard(
                        table = table,
                        onClick = {
                            // Navegar a detalles de la mesa
                            navController.navigate("table_details/${table.id}")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TablesSummary(
    totalTables: Int,
    occupiedTables: Int,
    freeTables: Int,
    totalOrders: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a2e).copy(alpha = 0.8f) // ✅ Aumenté la opacidad para mejor contraste
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // ✅ Agregué elevación para sombra
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Resumen del Restaurante",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TableStatItem(
                    count = totalTables,
                    label = "Total Mesas",
                    color = Color(0xFF2196F3)
                )
                TableStatItem(
                    count = occupiedTables,
                    label = "Ocupadas",
                    color = Color(0xFFFF9800)
                )
                TableStatItem(
                    count = freeTables,
                    label = "Libres",
                    color = Color(0xFF4CAF50)
                )
                TableStatItem(
                    count = totalOrders,
                    label = "Órdenes",
                    color = Color(0xFF9C27B0)
                )
            }
        }
    }
}

@Composable
fun TableStatItem(
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
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
    }
}
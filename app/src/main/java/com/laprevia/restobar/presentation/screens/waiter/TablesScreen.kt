package com.laprevia.restobar.presentation.screens.waiter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    val isInternetAvailable by viewModel.isInternetAvailable.collectAsState()
    val isFirebaseConnected by viewModel.isFirebaseConnected.collectAsState()
    val connectionMessage by viewModel.connectionMessage.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    // Texto de estado de conexión
    val connectionStatusText = when {
        !isInternetAvailable -> "🔴 SIN INTERNET - Mesas locales disponibles"
        !isFirebaseConnected -> "🟡 Reconectando con el servidor..."
        else -> "🟢 Conectado"
    }

    // ✅ Variable local para usar en condiciones
    val currentConnectionMessage = connectionMessage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // Banner de estado de conexión
        if (!isInternetAvailable || !isFirebaseConnected) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!isInternetAvailable)
                        Color(0xFFF44336).copy(alpha = 0.15f)
                    else
                        Color(0xFFFF9800).copy(alpha = 0.15f)
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
                        tint = if (!isInternetAvailable) Color(0xFFF44336) else Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = connectionStatusText,
                        color = if (!isInternetAvailable) Color(0xFFF44336) else Color(0xFFFF9800),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isInternetAvailable) {
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
        }

        // ✅ Mensaje temporal de conexión - CORREGIDO
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
                        if (!isInternetAvailable) "Sin conexión - Mostrando datos locales..." else "Cargando mesas...",
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
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "Sin mesas",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = if (!isInternetAvailable) "Sin conexión a internet" else "No hay mesas disponibles",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = if (!isInternetAvailable)
                            "Las mesas se sincronizarán cuando vuelva internet"
                        else
                            "Las mesas aparecerán aquí cuando estén configuradas",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    if (!isInternetAvailable) {
                        Button(
                            onClick = { viewModel.syncWithFirebase() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFe94560))
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        } else {
            TablesSummary(
                totalTables = tables.size,
                occupiedTables = tables.count { it.status == com.laprevia.restobar.data.model.TableStatus.OCUPADA },
                freeTables = tables.count { it.status == com.laprevia.restobar.data.model.TableStatus.LIBRE },
                totalOrders = orders.size,
                modifier = Modifier.padding(16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                items(tables.sortedBy { it.number }) { table ->
                    TableCard(
                        table = table,
                        onClick = {
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
            containerColor = Color(0xFF1a1a2e).copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
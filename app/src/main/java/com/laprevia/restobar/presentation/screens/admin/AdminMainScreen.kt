// AdminMainScreen.kt - VERSIÓN CORREGIDA
package com.laprevia.restobar.presentation.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.laprevia.restobar.presentation.viewmodel.AdminViewModel
import com.laprevia.restobar.presentation.viewmodel.LoginViewModel // ✅ IMPORTAR LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMainScreen(
    viewModel: AdminViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel(), // ✅ AGREGAR LoginViewModel
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val uiState = viewModel.uiState.collectAsState().value

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    ),
                color = Color(0xFF1a1a2e),
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Título principal
                    Column {
                        Text(
                            "LA PREVIA RESTOBAR",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Panel Administrativo",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    // ✅ BOTÓN CORREGIDO - AHORA SÍ CIERRA SESIÓN
                    IconButton(
                        onClick = {
                            println("🔄 AdminScreen: Cerrando sesión...")
                            loginViewModel.signOut() // ✅ LLAMAR AL VIEWMODEL CORRECTO
                            onLogout() // ✅ LLAMAR LA NAVEGACIÓN
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFe94560))
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Cerrar sesión",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showProductForm() },
                containerColor = Color(0xFFe94560),
                contentColor = Color.White,
                modifier = Modifier.shadow(8.dp, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Producto")
            }
        },
        containerColor = Color(0xFF0f3460)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0f3460),
                            Color(0xFF1a1a2e)
                        )
                    )
                )
        ) {
            // Header de bienvenida
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1a1a2e).copy(alpha = 0.8f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFFe94560),
                                        Color(0xFF1a1a2e)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = "Admin",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            "Panel de Administración",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Gestión completa del sistema",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Estadísticas rápidas con diseño mejorado - CORREGIDAS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProductStatCard(
                    title = "Total Productos",
                    value = uiState.products.size.toString(),
                    gradientColors = listOf(Color(0xFF4facfe), Color(0xFF00f2fe)),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                ProductStatCard(
                    title = "Activos",
                    value = uiState.products.count { it.isActive }.toString(),
                    gradientColors = listOf(Color(0xFF43e97b), Color(0xFF38f9d7)),
                    modifier = Modifier.weight(1f)
                )
            }

            // Segunda fila de estadísticas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ProductStatCard(
                    title = "Con Inventario",
                    value = uiState.products.count { it.trackInventory }.toString(),
                    gradientColors = listOf(Color(0xFFfa709a), Color(0xFFfee140)),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                ProductStatCard(
                    title = "Categorías",
                    value = uiState.categories.size.toString(),
                    gradientColors = listOf(Color(0xFFa8edea), Color(0xFFfed6e3)),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Lista de productos
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Productos Registrados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1a1a2e),
                        modifier = Modifier.padding(bottom = 16.dp)
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
                                    modifier = Modifier.size(64.dp),
                                    tint = Color(0xFF1a1a2e).copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No hay productos registrados",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color(0xFF1a1a2e).copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Presiona el botón + para agregar uno",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF1a1a2e).copy(alpha = 0.5f),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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

            // ✅ BOTÓN EXTRA DE DEBUG (opcional - puedes quitarlo después)
            Button(
                onClick = {
                    println("🔴 CERRANDO SESIÓN FORZADA")
                    loginViewModel.signOut()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("CERRAR SESIÓN (DEBUG)")
            }
        }
    }

    // Diálogos (sin cambios en la funcionalidad)
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
                Text("¿Estás seguro de que quieres eliminar \"${uiState.selectedProduct?.name}\"?")
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
fun ProductStatCard(
    title: String,
    value: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(gradientColors),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
package com.laprevia.restobar.presentation.screens.waiter.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laprevia.restobar.data.model.Product

@Composable
fun ProductItem(
    product: Product,
    onAddToOrder: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // ✅ CORREGIDO: Calcular propiedades que no existen en el modelo
    val isSellable = product.isActive && (!product.trackInventory || product.stock > 0)
    val currentStock = product.stock // Usar stock en lugar de initialQuantity

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header con nombre y precio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (product.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    if (product.category.isNotBlank()) {
                        Text(
                            text = product.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Text(
                    text = "S/. ${product.salePrice ?: 0.0}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Descripción
            if (product.description.isNotBlank()) {
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Stock y controles de cantidad
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Información de stock
                if (product.trackInventory) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stock: ${currentStock}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = when {
                                currentStock == 0.0 -> MaterialTheme.colorScheme.error
                                currentStock <= 5 -> Color(0xFFFFA000)
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            }
                        )

                        // Icono de advertencia si el stock es bajo
                        if (currentStock <= 5 && currentStock > 0) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Stock bajo",
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                } else {
                    // Espaciador cuando no hay control de inventario
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Botón para agregar al pedido
                // ✅ CORREGIDO: Usar isSellable calculado en lugar de la propiedad que no existe
                if (isSellable) {
                    Button(
                        onClick = onAddToOrder,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Agregar",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agregar")
                    }
                } else {
                    Text(
                        text = when {
                            !product.isActive -> "INACTIVO"
                            product.trackInventory && currentStock <= 0 -> "SIN STOCK"
                            else -> "NO DISPONIBLE"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Mensaje de stock bajo
            if (product.trackInventory && currentStock <= 5 && currentStock > 0) {
                Text(
                    text = "⚠️ Stock bajo",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFA000),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

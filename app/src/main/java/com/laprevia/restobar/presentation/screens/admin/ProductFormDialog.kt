package com.laprevia.restobar.presentation.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.ProductValidation
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    product: Product?,
    categories: List<String>,
    onSave: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var salePrice by remember { mutableStateOf(product?.salePrice?.toString() ?: "") }
    var costPrice by remember { mutableStateOf(product?.costPrice?.toString() ?: "") }
    var trackInventory by remember { mutableStateOf(product?.trackInventory ?: false) }
    var stock by remember { mutableStateOf(product?.stock?.toString() ?: "0.0") }
    var minStock by remember { mutableStateOf(product?.minStock?.toString() ?: "0.0") }
    var imageUrl by remember { mutableStateOf(product?.imageUrl ?: "") }
    var isActive by remember { mutableStateOf(product?.isActive ?: true) }
    var category by remember { mutableStateOf(product?.category ?: "") }

    val isEditMode = product != null
    val salePriceValue = ProductValidation.parseDecimal(salePrice)
    val costPriceValue = ProductValidation.parseDecimal(costPrice)
    val stockValue = ProductValidation.parseDecimal(stock)
    val minStockValue = if (minStock.isBlank()) 0.0 else ProductValidation.parseDecimal(minStock)
    val salePriceError = if (salePriceValue == null) "Ingresa un precio numérico no negativo." else null
    val costPriceError = if (costPriceValue == null) "Ingresa un costo numérico no negativo." else null
    val stockError = if (trackInventory && stockValue == null) "Ingresa un stock numérico no negativo." else null
    val minStockError = if (trackInventory && minStockValue == null) "Ingresa un mínimo numérico no negativo." else null

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditMode) "Editar Producto" else "Nuevo Producto",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Campos del formulario según tu modelo actual
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = name.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Precios
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = salePrice,
                        onValueChange = { value ->
                            if ('-' !in value) salePrice = ProductValidation.filterNumericInput(value)
                        },
                        label = { Text("Precio Venta *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = salePriceError != null,
                        supportingText = salePriceError?.let { error -> { Text(error) } }
                    )

                    OutlinedTextField(
                        value = costPrice,
                        onValueChange = { value ->
                            if ('-' !in value) costPrice = ProductValidation.filterNumericInput(value)
                        },
                        label = { Text("Precio Costo *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = costPriceError != null,
                        supportingText = costPriceError?.let { error -> { Text(error) } }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Toggle para inventario
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Controlar inventario?")
                    Switch(
                        checked = trackInventory,
                        onCheckedChange = { trackInventory = it }
                    )
                }

                if (trackInventory) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Configuración de Inventario",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = stock,
                            onValueChange = { value ->
                                if ('-' !in value) stock = ProductValidation.filterNumericInput(value)
                            },
                            label = { Text("Stock Actual *") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = stockError != null,
                            supportingText = stockError?.let { error -> { Text(error) } }
                        )

                        OutlinedTextField(
                            value = minStock,
                            onValueChange = { value ->
                                if ('-' !in value) minStock = ProductValidation.filterNumericInput(value)
                            },
                            label = { Text("Stock Mínimo") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("0.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = minStockError != null,
                            supportingText = minStockError?.let { error -> { Text(error) } }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Categoría
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoría *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = category.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // URL de imagen
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("URL de Imagen (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("https://ejemplo.com/imagen.jpg") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Estado activo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Producto activo?")
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newProduct = Product(
                                id = product?.id ?: UUID.randomUUID().toString(),
                                name = name,
                                description = description,
                                category = category,
                                salePrice = salePriceValue,
                                costPrice = costPriceValue,
                                trackInventory = trackInventory,
                                stock = if (trackInventory) stockValue ?: 0.0 else 0.0,
                                minStock = if (trackInventory) minStockValue ?: 0.0 else 0.0,
                                imageUrl = imageUrl.ifBlank { null },
                                isActive = isActive,
                                createdAt = product?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(newProduct)
                        },
                        enabled = name.isNotBlank() &&
                                category.isNotBlank() &&
                                salePriceError == null &&
                                costPriceError == null &&
                                stockError == null &&
                                minStockError == null
                    ) {
                        Text(if (isEditMode) "Actualizar" else "Crear")
                    }
                }
            }
        }
    }
}

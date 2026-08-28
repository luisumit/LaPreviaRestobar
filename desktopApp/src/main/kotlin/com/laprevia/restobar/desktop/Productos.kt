package com.laprevia.restobar.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.model.Money
import com.laprevia.restobar.domain.repository.FirebaseProductRepository
import com.laprevia.restobar.platform.currentTimeMillis
import com.laprevia.restobar.platform.randomUuid
import kotlinx.coroutines.launch

/**
 * Gestion de productos desde la PC: buscar, crear, editar, activar/desactivar y
 * eliminar — con teclado. Usa los mismos repos compartidos que el celular.
 */
@Composable
fun ProductosView(products: List<Product>, productRepo: FirebaseProductRepository) {
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<Product?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Product?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    if (creating) {
        ProductFormDialog(
            original = null,
            onSave = { product ->
                scope.launch {
                    runCatching { productRepo.createProduct(product) }
                        .onSuccess { status = "✅ Producto '${product.name}' creado" }
                        .onFailure { status = "❌ ${it.message}" }
                }
                creating = false
            },
            onDismiss = { creating = false }
        )
    }
    editing?.let { product ->
        ProductFormDialog(
            original = product,
            onSave = { updated ->
                scope.launch {
                    runCatching { productRepo.updateProduct(updated) }
                        .onSuccess { status = "✅ Producto '${updated.name}' actualizado" }
                        .onFailure { status = "❌ ${it.message}" }
                }
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
    deleting?.let { product ->
        Dialog(onDismissRequest = { deleting = null }) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.width(380.dp).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("¿Eliminar '${product.name}'?", fontWeight = FontWeight.Bold, color = Color(0xFFF5F5F5))
                    Text("Esta accion no se puede deshacer.", color = Color(0xFFFF6E40), fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { deleting = null }, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                        Button(
                            onClick = {
                                scope.launch {
                                    runCatching { productRepo.deleteProduct(product.id) }
                                        .onSuccess { status = "🗑️ '${product.name}' eliminado" }
                                        .onFailure { status = "❌ ${it.message}" }
                                }
                                deleting = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                            modifier = Modifier.weight(1f)
                        ) { Text("Eliminar") }
                    }
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                label = { Text("Buscar producto...") }, singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { creating = true }, modifier = Modifier.height(52.dp)) {
                Text("+ Nuevo producto", fontWeight = FontWeight.Bold)
            }
        }
        status?.let { Text(it, color = Color(0xFFFFB300), fontSize = 13.sp) }

        val filtered = products
            .filter { search.isBlank() || it.name.contains(search, true) || it.category.contains(search, true) }
            .sortedBy { it.name }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.id }) { product ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)), shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(product.name, fontWeight = FontWeight.SemiBold, color = Color(0xFFF5F5F5), fontSize = 15.sp)
                                if (!product.isActive) {
                                    Text("INACTIVO", color = Color(0xFFFF6E40), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                "${product.category}  ·  ${Money(product.salePrice ?: 0.0).formatted()}" +
                                    if (product.trackInventory) "  ·  stock: ${product.stock}" else "",
                                color = Color(0xFFF5F5F5).copy(alpha = 0.6f), fontSize = 12.sp
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { editing = product }) { Text("Editar", fontSize = 12.sp) }
                            OutlinedButton(onClick = {
                                scope.launch {
                                    runCatching { productRepo.updateProductStatus(product.id, !product.isActive) }
                                        .onSuccess { status = if (product.isActive) "⏸ '${product.name}' desactivado" else "▶ '${product.name}' activado" }
                                }
                            }) { Text(if (product.isActive) "Desactivar" else "Activar", fontSize = 12.sp) }
                            OutlinedButton(onClick = { deleting = product }) { Text("Eliminar", fontSize = 12.sp, color = Color(0xFFFF5252)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductFormDialog(original: Product?, onSave: (Product) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(original?.name ?: "") }
    var category by remember { mutableStateOf(original?.category ?: "") }
    var description by remember { mutableStateOf(original?.description ?: "") }
    var salePrice by remember { mutableStateOf(original?.salePrice?.toString() ?: "") }
    var costPrice by remember { mutableStateOf(original?.costPrice?.toString() ?: "") }
    var stock by remember { mutableStateOf(original?.stock?.toString() ?: "0") }
    var minStock by remember { mutableStateOf(original?.minStock?.toString() ?: "0") }
    var trackInventory by remember { mutableStateOf(original?.trackInventory ?: false) }
    var isActive by remember { mutableStateOf(original?.isActive ?: true) }

    fun num(s: String): Double? = s.replace(",", ".").toDoubleOrNull()

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)), shape = RoundedCornerShape(16.dp)) {
            Column(
                Modifier.width(460.dp).heightIn(max = 640.dp).padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    if (original == null) "Nuevo producto" else "Editar: ${original.name}",
                    fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color(0xFFF5F5F5)
                )
                OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("Categoria") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(description, { description = it }, label = { Text("Descripcion") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(salePrice, { salePrice = it }, label = { Text("Precio venta (S/)") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(costPrice, { costPrice = it }, label = { Text("Precio costo (S/)") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Controlar inventario", color = Color(0xFFF5F5F5), modifier = Modifier.weight(1f))
                    Switch(checked = trackInventory, onCheckedChange = { trackInventory = it })
                }
                if (trackInventory) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(stock, { stock = it }, label = { Text("Stock") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(minStock, { minStock = it }, label = { Text("Stock minimo") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Activo (visible en el menu)", color = Color(0xFFF5F5F5), modifier = Modifier.weight(1f))
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(
                        onClick = {
                            val now = currentTimeMillis()
                            val product = (original ?: Product(id = randomUuid(), createdAt = now)).copy(
                                name = name.trim(),
                                category = category.trim().ifBlank { "General" },
                                description = description.trim(),
                                salePrice = num(salePrice),
                                costPrice = num(costPrice),
                                trackInventory = trackInventory,
                                stock = num(stock) ?: 0.0,
                                minStock = num(minStock) ?: 0.0,
                                isActive = isActive,
                                updatedAt = now
                            )
                            onSave(product)
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) { Text("Guardar") }
                }
            }
        }
    }
}

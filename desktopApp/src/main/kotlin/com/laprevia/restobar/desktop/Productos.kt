package com.laprevia.restobar.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
            LpDialogCard(width = 380) {
                Text("¿Eliminar '${product.name}'?", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Lp.Text)
                Text("Esta acción no se puede deshacer.", color = Lp.Coral, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { deleting = null },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Lp.FieldBorder),
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancelar", color = Lp.TextSoft) }
                    Button(
                        onClick = {
                            scope.launch {
                                runCatching { productRepo.deleteProduct(product.id) }
                                    .onSuccess { status = "🗑️ '${product.name}' eliminado" }
                                    .onFailure { status = "❌ ${it.message}" }
                            }
                            deleting = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Lp.Red),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = search, onValueChange = { search = it },
                placeholder = { Text("Buscar producto...", color = Lp.TextMuted) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Lp.TextDim, modifier = Modifier.size(18.dp)) },
                shape = RoundedCornerShape(13.dp),
                colors = lpFieldColors(),
                modifier = Modifier.weight(1f)
            )
            Box(
                Modifier.height(52.dp).clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(listOf(Lp.Amber, Lp.AmberDeep)))
                    .lpHover(0.10f)
                    .clickable { creating = true }
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("+ NUEVO PRODUCTO", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp, color = Lp.OnAccent)
            }
        }
        status?.let { Text(it, color = Lp.Amber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }

        val filtered = products
            .filter { search.isBlank() || it.name.contains(search, true) || it.category.contains(search, true) }
            .sortedBy { it.name.lowercase() }

        Text(
            "${filtered.size} de ${products.size} productos" +
                "  ·  ${products.count { it.isActive }} activos",
            color = Lp.TextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
        )

        val listState = rememberLazyListState()
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize().padding(end = 14.dp)
            ) {
            items(filtered, key = { it.id }) { product ->
                Row(
                    Modifier.fillMaxWidth().lpCard(12.dp)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(product.name, fontWeight = FontWeight.Bold, color = Lp.Text, fontSize = 15.sp)
                            if (!product.isActive) StatusPill("INACTIVO", Lp.Coral)
                        }
                        Text(
                            "${product.category}  ·  ${Money(product.salePrice ?: 0.0).formatted()}" +
                                if (product.trackInventory) "  ·  stock: ${product.stock}" else "",
                            color = Lp.TextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProductAction("Editar") { editing = product }
                        ProductAction(if (product.isActive) "Desactivar" else "Activar") {
                            scope.launch {
                                runCatching { productRepo.updateProductStatus(product.id, !product.isActive) }
                                    .onSuccess { status = if (product.isActive) "⏸ '${product.name}' desactivado" else "▶ '${product.name}' activado" }
                            }
                        }
                        ProductAction("Eliminar", color = Lp.Red) { deleting = product }
                    }
                }
            }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
            )
        }
    }
}

@Composable
private fun ProductAction(label: String, color: androidx.compose.ui.graphics.Color = Lp.TextSoft, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (color == Lp.Red) Lp.Red.copy(alpha = 0.4f) else Lp.FieldBorder),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color) }
}

/** Contenedor comun de los dialogos del panel (tarjeta oscura con borde sutil). */
@Composable
fun LpDialogCard(width: Int, maxHeight: Int? = null, content: @Composable ColumnScope.() -> Unit) {
    val scroll = rememberScrollState()
    var modifier = Modifier.width(width.dp)
    if (maxHeight != null) modifier = modifier.heightIn(max = maxHeight.dp)
    Column(
        // padding ANTES del scroll: el marco queda fijo y el contenido scrollea adentro
        modifier.lpCard(18.dp)
            .padding(22.dp)
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
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
        LpDialogCard(width = 460, maxHeight = 640) {
            Text(
                if (original == null) "NUEVO PRODUCTO" else "EDITAR PRODUCTO",
                fontFamily = BebasFamily, fontSize = 24.sp, letterSpacing = 1.5.sp, color = Lp.Text
            )
            original?.let { Text(it.name, color = Lp.TextDim, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            OutlinedTextField(name, { name = it }, label = { Text("Nombre *") }, singleLine = true, shape = RoundedCornerShape(13.dp), colors = lpFieldColors(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(category, { category = it }, label = { Text("Categoría") }, singleLine = true, shape = RoundedCornerShape(13.dp), colors = lpFieldColors(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text("Descripción") }, shape = RoundedCornerShape(13.dp), colors = lpFieldColors(), modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(salePrice, { salePrice = it }, label = { Text("Precio venta (S/)") }, singleLine = true, shape = RoundedCornerShape(13.dp), colors = lpFieldColors(), modifier = Modifier.weight(1f))
                OutlinedTextField(costPrice, { costPrice = it }, label = { Text("Precio costo (S/)") }, singleLine = true, shape = RoundedCornerShape(13.dp), colors = lpFieldColors(), modifier = Modifier.weight(1f))
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Controlar inventario", color = Lp.Text, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Switch(checked = trackInventory, onCheckedChange = { trackInventory = it })
            }
            if (trackInventory) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(stock, { stock = it }, label = { Text("Stock") }, singleLine = true, shape = RoundedCornerShape(13.dp), colors = lpFieldColors(), modifier = Modifier.weight(1f))
                    OutlinedTextField(minStock, { minStock = it }, label = { Text("Stock mínimo") }, singleLine = true, shape = RoundedCornerShape(13.dp), colors = lpFieldColors(), modifier = Modifier.weight(1f))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Activo (visible en el menú)", color = Lp.Text, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Lp.FieldBorder),
                    modifier = Modifier.weight(1f)
                ) { Text("Cancelar", color = Lp.TextSoft) }
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
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("Guardar", fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

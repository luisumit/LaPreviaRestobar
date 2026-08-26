package com.laprevia.restobar.data.repository

import com.laprevia.restobar.data.model.Inventory
import com.laprevia.restobar.domain.repository.FirebaseInventoryRepository
import com.laprevia.restobar.platform.currentTimeMillis
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.DataSnapshot
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

/**
 * Repositorio de inventario MULTIPLATAFORMA (GitLive Firebase): Android, Desktop/JVM y Web JS.
 * Conserva EXACTAMENTE el mismo formato de datos que la implementacion nativa.
 */
class GitLiveInventoryRepository : FirebaseInventoryRepository {

    private val inventoryRef: DatabaseReference get() = Firebase.database.reference("inventory")

    // ==================== LISTADO / TIEMPO REAL ====================

    override fun getInventory(): Flow<List<Inventory>> =
        inventoryRef.valueEvents.map { snapshot -> snapshot.children.mapNotNull { it.toInventory() } }

    override fun getLowStockItems(): Flow<List<Inventory>> =
        getInventory().map { items -> items.filter { it.currentStock <= it.minimumStock } }

    override fun getLowStockAlerts(): Flow<List<Inventory>> =
        getInventory().map { items ->
            items.filter { it.currentStock <= it.minimumStock && it.currentStock > 0 }
        }

    override fun getInventoryByCategory(category: String): Flow<List<Inventory>> =
        inventoryRef.orderByChild("category").equalTo(category).valueEvents
            .map { snapshot -> snapshot.children.mapNotNull { it.toInventory() } }

    override fun listenToInventoryChanges(): Flow<Inventory> =
        getInventory().mapNotNull { it.lastOrNull() }

    // ==================== ACTUALIZACIONES ====================

    override suspend fun updateStock(productId: String, newQuantity: Double) {
        inventoryRef.child(productId).updateChildren(mapOf("currentStock" to newQuantity))
    }

    override suspend fun addInventoryItem(item: Inventory) {
        inventoryRef.child(item.productId).updateChildren(
            mapOf(
                "productId" to item.productId,
                "productName" to item.productName,
                "currentStock" to item.currentStock,
                "unitOfMeasure" to item.unitOfMeasure,
                "minimumStock" to item.minimumStock,
                "category" to (item.category ?: ""),
                "createdAt" to currentTimeMillis()
            )
        )
    }

    override suspend fun deleteInventoryItem(productId: String) {
        val snapshot = inventoryRef.child(productId).valueEvents.first()
        if (snapshot.exists) {
            inventoryRef.child(productId).removeValue()
        }
    }

    override suspend fun deleteProduct(productId: String) = deleteInventoryItem(productId)

    override suspend fun updateInventoryFields(productId: String, updates: Map<String, Any>) {
        val snapshot = inventoryRef.child(productId).valueEvents.first()
        if (!snapshot.exists) {
            val fullData = mutableMapOf<String, Any>()
            fullData.putAll(updates)
            fullData["productId"] = productId
            inventoryRef.child(productId).updateChildren(fullData)
        } else {
            inventoryRef.child(productId).updateChildren(updates)
        }
    }

    // ==================== BUSQUEDA ====================

    override suspend fun getInventoryItemById(productId: String): Inventory? = runCatching {
        inventoryRef.child(productId).valueEvents.first().toInventory()
    }.getOrNull()

    override suspend fun getCurrentStock(productId: String): Double = runCatching {
        (inventoryRef.child(productId).child("currentStock").valueEvents.first().value as? Number)
            ?.toDouble() ?: 0.0
    }.getOrDefault(0.0)

    override suspend fun initializeDefaultInventory() {
        // Igual que la implementacion nativa: el inventario se sincroniza desde los
        // productos con trackInventory = true; aqui solo se verifica el estado.
        runCatching { inventoryRef.valueEvents.first().children.count() }
    }

    // ==================== CONVERSION (mismo wire format que el SDK nativo) ====================

    private fun DataSnapshot.toInventory(): Inventory? = runCatching {
        val productId = key ?: return null
        Inventory(
            productId = productId,
            productName = child("productName").value as? String ?: "",
            currentStock = (child("currentStock").value as? Number)?.toDouble() ?: 0.0,
            unitOfMeasure = child("unitOfMeasure").value as? String ?: "unidades",
            minimumStock = (child("minimumStock").value as? Number)?.toDouble() ?: 0.0,
            category = child("category").value as? String
        )
    }.getOrNull()
}

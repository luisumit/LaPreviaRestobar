package com.laprevia.restobar.repositories

import com.laprevia.restobar.data.model.Inventory
import com.laprevia.restobar.domain.repository.FirebaseInventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeInventoryRepository : FirebaseInventoryRepository {
    private val inventory = mutableListOf<Inventory>()

    // ==================== FirebaseInventoryRepository ====================
    override fun listenToInventoryChanges(): Flow<Inventory> = flowOf()
    override fun getLowStockAlerts(): Flow<List<Inventory>> = flowOf(inventory.filter { it.currentStock <= it.minimumStock })
    override suspend fun initializeDefaultInventory() = Unit
    override suspend fun getCurrentStock(productId: String): Double = inventory.find { it.productId == productId }?.currentStock ?: 0.0
    override suspend fun updateInventoryFields(productId: String, updates: Map<String, Any>) {
        val index = inventory.indexOfFirst { it.productId == productId }
        if (index != -1) {
            val item = inventory[index]
            val newStock = updates["currentStock"] as? Double ?: item.currentStock
            inventory[index] = item.copy(currentStock = newStock)
        }
    }
    override suspend fun deleteProduct(productId: String) { inventory.removeAll { it.productId == productId } }

    // ==================== InventoryRepository ====================
    override fun getInventory(): Flow<List<Inventory>> = flowOf(inventory)
    override fun getLowStockItems(): Flow<List<Inventory>> = flowOf(inventory.filter { it.currentStock <= it.minimumStock })
    override fun getInventoryByCategory(category: String): Flow<List<Inventory>> = flowOf(inventory.filter { it.category == category })
    override suspend fun updateStock(productId: String, newQuantity: Double) {
        val index = inventory.indexOfFirst { it.productId == productId }
        if (index != -1) {
            inventory[index] = inventory[index].copy(currentStock = newQuantity)
        }
    }
    override suspend fun addInventoryItem(item: Inventory) { inventory.add(item) }
    override suspend fun deleteInventoryItem(productId: String) { inventory.removeAll { it.productId == productId } }
    override suspend fun getInventoryItemById(productId: String): Inventory? = inventory.find { it.productId == productId }
}
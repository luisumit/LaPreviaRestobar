package com.laprevia.restobar.domain.repository

import com.laprevia.restobar.data.model.Inventory
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun getInventory(): Flow<List<Inventory>>
    fun getLowStockItems(): Flow<List<Inventory>>
    suspend fun updateStock(productId: String, newQuantity: Double)
    fun getInventoryByCategory(category: String): Flow<List<Inventory>> // 🔥 SIN 'suspend'
}
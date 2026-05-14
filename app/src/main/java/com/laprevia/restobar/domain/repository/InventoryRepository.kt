package com.laprevia.restobar.domain.repository

import com.laprevia.restobar.data.model.Inventory
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {

    // ==================== LISTADO PRINCIPAL ====================
    fun getInventory(): Flow<List<Inventory>>

    // ==================== ALERTAS ====================
    fun getLowStockItems(): Flow<List<Inventory>>

    // ==================== FILTROS ====================
    fun getInventoryByCategory(category: String): Flow<List<Inventory>>

    // ==================== ACTUALIZACIONES ====================
    suspend fun updateStock(productId: String, newQuantity: Double)

    // ==================== CRUD BASE (RECOMENDADO PARA OFFLINE) ====================
    suspend fun addInventoryItem(item: Inventory)
    suspend fun deleteInventoryItem(productId: String)
    suspend fun getInventoryItemById(productId: String): Inventory?
}

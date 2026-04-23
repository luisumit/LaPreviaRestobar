package com.laprevia.restobar.domain.repository

import com.laprevia.restobar.data.model.Inventory
import kotlinx.coroutines.flow.Flow

interface FirebaseInventoryRepository : InventoryRepository {
    // Métodos específicos para Firebase (tiempo real)
    fun listenToInventoryChanges(): Flow<Inventory>
    fun getLowStockAlerts(): Flow<List<Inventory>>
    suspend fun initializeDefaultInventory()

    // ✅ MÉTODOS EXISTENTES
    suspend fun getCurrentStock(productId: String): Double

    // ✅ NUEVO MÉTODO: Actualizar múltiples campos de un producto
    suspend fun updateInventoryFields(productId: String, updates: Map<String, Any>)

    // ✅ NUEVO MÉTODO: Eliminar un producto del inventario
    suspend fun deleteProduct(productId: String)
}
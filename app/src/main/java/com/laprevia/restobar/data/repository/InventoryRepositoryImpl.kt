package com.laprevia.restobar.data.repository

import com.laprevia.restobar.data.local.dao.InventoryDao
import com.laprevia.restobar.data.local.entity.InventoryEntity
import javax.inject.Inject
import javax.inject.Singleton
import com.laprevia.restobar.data.mapper.*

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val inventoryDao: InventoryDao
) {

    suspend fun updateInventory(item: InventoryEntity) {
        inventoryDao.insert(item.copy(syncStatus = "PENDING"))
    }

    suspend fun getAll() = inventoryDao.getAll()

    suspend fun getPending() = inventoryDao.getPending()

    suspend fun updateStatus(id: String, status: String) {
        inventoryDao.updateStatus(id, status)
    }
}
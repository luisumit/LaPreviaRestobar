package com.laprevia.restobar.data.local.dao

import androidx.room.*
import com.laprevia.restobar.data.local.entity.InventoryEntity

@Dao
interface InventoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryEntity)

    @Query("SELECT * FROM inventory")
    suspend fun getAll(): List<InventoryEntity>

    @Query("SELECT * FROM inventory WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<InventoryEntity>

    @Query("UPDATE inventory SET syncStatus = :status WHERE productId = :id")
    suspend fun updateStatus(id: String, status: String)

    // ✅ NUEVO: Actualizar con versión
    @Query("UPDATE inventory SET syncStatus = :status, version = :newVersion, lastModified = :lastModified WHERE productId = :id AND version < :newVersion")
    suspend fun updateStatusIfNewer(id: String, status: String, newVersion: Long, lastModified: Long)

    // ✅ NUEVO: Obtener por ID
    @Query("SELECT * FROM inventory WHERE productId = :id")
    suspend fun getById(id: String): InventoryEntity?
    // ✅ AGREGAR ESTOS MÉTODOS
    @Query("DELETE FROM inventory WHERE productId = :productId")
    suspend fun deleteProduct(productId: String)


}
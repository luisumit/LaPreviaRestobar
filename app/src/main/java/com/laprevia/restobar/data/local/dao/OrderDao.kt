package com.laprevia.restobar.data.local.dao

import androidx.room.*
import com.laprevia.restobar.data.local.entity.OrderEntity

@Dao
interface OrderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity)

    @Query("SELECT * FROM orders")
    suspend fun getAll(): List<OrderEntity>

    @Query("SELECT * FROM orders WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<OrderEntity>

    @Query("UPDATE orders SET syncStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    // ✅ NUEVO: Actualizar con versión
    @Query("UPDATE orders SET syncStatus = :status, version = :newVersion, lastModified = :lastModified WHERE id = :id AND version < :newVersion")
    suspend fun updateStatusIfNewer(id: String, status: String, newVersion: Long, lastModified: Long)

    // ✅ NUEVO: Obtener por ID
    @Query("SELECT * FROM orders WHERE id = :id")
    suspend fun getById(id: String): OrderEntity?
    // ✅ AGREGAR ESTE MÉTODO
    @Query("DELETE FROM orders WHERE id = :orderId")
    suspend fun deleteOrder(orderId: String)
}

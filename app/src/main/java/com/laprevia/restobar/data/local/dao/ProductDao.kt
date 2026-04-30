package com.laprevia.restobar.data.local.dao

import androidx.room.*
import com.laprevia.restobar.data.local.entity.ProductEntity

@Dao
interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: ProductEntity)

    @Query("SELECT * FROM products")
    suspend fun getAll(): List<ProductEntity>

    @Query("SELECT * FROM products WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<ProductEntity>

    @Query("UPDATE products SET syncStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)


    // ✅ NUEVO: Actualizar con versión
    @Query("UPDATE products SET syncStatus = :status, version = :newVersion, lastModified = :lastModified WHERE id = :id AND version < :newVersion")
    suspend fun updateStatusIfNewer(id: String, status: String, newVersion: Long, lastModified: Long)

    // ✅ NUEVO: Obtener por ID
    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getById(id: String): ProductEntity?
    // ✅ AGREGAR ESTE MÉTODO PARA ELIMINAR
    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: String)


}
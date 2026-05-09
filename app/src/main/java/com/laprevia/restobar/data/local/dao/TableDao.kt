package com.laprevia.restobar.data.local.dao

import androidx.room.*
import com.laprevia.restobar.data.local.entity.TableEntity

@Dao
interface TableDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(table: TableEntity)

    @Query("SELECT * FROM tables")
    suspend fun getAll(): List<TableEntity>

    @Query("SELECT * FROM tables WHERE syncStatus = 'PENDING'")
    suspend fun getPending(): List<TableEntity>

    @Query("UPDATE tables SET syncStatus = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    // ✅ NUEVO: Actualizar con versión
    @Query("UPDATE tables SET syncStatus = :status, version = :newVersion, lastModified = :lastModified WHERE id = :id AND version < :newVersion")
    suspend fun updateStatusIfNewer(id: Int, status: String, newVersion: Long, lastModified: Long)

    // ✅ NUEVO: Obtener por ID
    @Query("SELECT * FROM tables WHERE id = :id")
    suspend fun getById(id: Int): TableEntity?
}

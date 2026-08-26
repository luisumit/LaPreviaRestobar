package com.laprevia.restobar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laprevia.restobar.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC LIMIT :limit")
    fun getLatestFlow(limit: Int = 80): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY createdAt DESC")
    suspend fun getAll(): List<AuditLogEntity>
}

package com.laprevia.restobar.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laprevia.restobar.data.local.entity.CashClosureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashClosureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(closure: CashClosureEntity)

    @Query("SELECT * FROM cash_closures ORDER BY createdAt DESC LIMIT :limit")
    fun getLatestFlow(limit: Int = 30): Flow<List<CashClosureEntity>>

    @Query("SELECT * FROM cash_closures ORDER BY createdAt DESC")
    suspend fun getAll(): List<CashClosureEntity>
}

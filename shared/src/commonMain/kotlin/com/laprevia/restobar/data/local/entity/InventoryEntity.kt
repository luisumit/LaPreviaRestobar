package com.laprevia.restobar.data.local.entity

import com.laprevia.restobar.platform.currentTimeMillis
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory")
data class InventoryEntity(

    @PrimaryKey
    val productId: String,

    val productName: String,
    val currentStock: Double,
    val unitOfMeasure: String,

    val minimumStock: Double,
    val category: String?,

    val syncStatus: String = "PENDING",
    val version: Long = currentTimeMillis(),  // ✅ NUEVO
    val lastModified: Long = currentTimeMillis()  // ✅ NUEVO
)

package com.laprevia.restobar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(

    @PrimaryKey
    val id: String,

    val tableId: Int,
    val tableNumber: Int,

    val status: String,
    val total: Double,

    val createdAt: Long,
    val updatedAt: Long,

    val waiterId: String?,
    val waiterName: String?,

    val notes: String?,
    val itemsJson: String = "[]",  // Agregar esto
    val syncStatus: String = "PENDING",
    val version: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

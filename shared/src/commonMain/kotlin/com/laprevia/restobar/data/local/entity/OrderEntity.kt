package com.laprevia.restobar.data.local.entity

import com.laprevia.restobar.platform.currentTimeMillis
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
    val paymentMethod: String = "UNSPECIFIED",
    val paidAt: Long? = null,
    val receiptNumber: String? = null,
    val amountReceived: Double? = null,
    val changeGiven: Double? = null,
    val discountAmount: Double? = null,
    val discountReason: String? = null,
    val itemsJson: String = "[]",  // Agregar esto
    val syncStatus: String = "PENDING",
    val version: Long = currentTimeMillis(),
    val lastModified: Long = currentTimeMillis()
)

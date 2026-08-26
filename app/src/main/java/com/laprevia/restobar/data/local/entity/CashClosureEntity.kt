package com.laprevia.restobar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cash_closures")
data class CashClosureEntity(
    @PrimaryKey
    val id: String,
    val periodStart: Long,
    val periodEnd: Long,
    val totalSales: Double,
    val grossProfit: Double,
    val chargedOrders: Int,
    val cancelledOrders: Int,
    val productsSold: Int,
    val cashSales: Double = 0.0,
    val yapePlinSales: Double = 0.0,
    val cardSales: Double = 0.0,
    val bestSellingProduct: String = "Sin ventas",
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis()
)

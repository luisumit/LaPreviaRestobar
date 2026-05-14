package com.laprevia.restobar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(

    @PrimaryKey
    val id: String,

    val name: String,
    val description: String,
    val category: String,

    val salePrice: Double?,
    val costPrice: Double?,

    val trackInventory: Boolean,
    val stock: Double,
    val minStock: Double,

    val isActive: Boolean,

    val syncStatus: String = "PENDING",
    val version: Long = System.currentTimeMillis(),  // ✅ NUEVO
    val lastModified: Long = System.currentTimeMillis(),  // ✅ NUEVO
    val updatedAt: Long = System.currentTimeMillis()  // ✅ AGREGAR ESTO
)

package com.laprevia.restobar.data.local.entity

import com.laprevia.restobar.platform.currentTimeMillis
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "tables")
data class TableEntity(

    @PrimaryKey
    val id: Int,

    val number: Int,

    val status: String, // ✔ sigue siendo String

    val currentOrderId: String?,

    val capacity: Int,

    val syncStatus: String = "PENDING",
    val version: Long = currentTimeMillis(),  // ✅ NUEVO
    val lastModified: Long = currentTimeMillis()  // ✅ NUEVO
)

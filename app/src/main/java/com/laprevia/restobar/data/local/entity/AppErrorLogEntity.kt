package com.laprevia.restobar.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_error_logs")
data class AppErrorLogEntity(
    @PrimaryKey
    val id: String,
    val source: String,
    val message: String,
    val detail: String,
    val createdAt: Long = System.currentTimeMillis()
)

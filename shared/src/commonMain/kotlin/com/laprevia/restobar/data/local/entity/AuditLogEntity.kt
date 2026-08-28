package com.laprevia.restobar.data.local.entity

import com.laprevia.restobar.platform.currentTimeMillis
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey
    val id: String,
    val action: String,
    val actorRole: String,
    val actorName: String,
    val targetType: String,
    val targetId: String,
    val detail: String,
    val createdAt: Long = currentTimeMillis()
)

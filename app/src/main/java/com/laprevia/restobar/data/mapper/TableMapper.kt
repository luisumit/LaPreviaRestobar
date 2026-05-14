package com.laprevia.restobar.data.mapper

import com.laprevia.restobar.data.local.entity.TableEntity
import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.data.model.TableStatus

// ROOM → DOMAIN
fun TableEntity.toDomain(): Table {
    return Table(
        id = id,
        number = number,
        status = TableStatus.valueOf(
            status.uppercase() // ✔ evita errores por minúsculas
        ),
        currentOrderId = currentOrderId,
        capacity = capacity,
        version = version  // ✅ NUEVO
    )
}

// DOMAIN → ROOM
fun Table.toEntity(): TableEntity {
    return TableEntity(
        id = id,
        number = number,
        status = status.name, // ✔ LIBRE / OCUPADA / RESERVADA
        currentOrderId = currentOrderId,
        capacity = capacity,
        syncStatus = "PENDING",
                version = System.currentTimeMillis(),  // ✅ NUEVO
        lastModified = System.currentTimeMillis()  // ✅ NUEVO
    )
}

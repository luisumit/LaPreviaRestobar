// data/remote/dto/TableDto.kt - CORREGIDO
package com.laprevia.restobar.data.remote.dto

import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.data.model.TableStatus

data class TableDto(
    val id: Int,
    val number: Int,
    val status: String,
    val currentOrderId: String?
) {
    fun toTable(): Table {
        return Table(
            id = id,
            number = number,
            status = when(status.uppercase()) {
                "AVAILABLE", "LIBRE" -> TableStatus.LIBRE
                "OCCUPIED", "OCUPADA" -> TableStatus.OCUPADA

                else -> TableStatus.LIBRE
            },
            currentOrderId = currentOrderId
        )
        // ✅ ELIMINADO: capacity = 4 (no existe en tu modelo Table)
    }
}
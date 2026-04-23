// data/remote/dto/OrderDto.kt - CORREGIDO
package com.laprevia.restobar.data.remote.dto

import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus

data class OrderDto(
    val id: String,
    val tableId: Int, // ✅ Ya es Int, igual que tu Order model
    val tableNumber: Int,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val total: Double
) {
    fun toOrder(): Order {
        return Order(
            id = id,
            tableId = tableId, // ✅ Ya no necesitas conversión
            tableNumber = tableNumber,
            items = emptyList(), // Los items vendrían en otro endpoint
            status = when(status.uppercase()) {
                "PENDING", "ENVIADO" -> OrderStatus.ENVIADO
                "CONFIRMED", "ACEPTADO" -> OrderStatus.ACEPTADO
                "PREPARING", "EN_PREPARACION" -> OrderStatus.EN_PREPARACION
                "READY", "LISTO" -> OrderStatus.LISTO
                else -> OrderStatus.ENVIADO
            },
            total = total,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
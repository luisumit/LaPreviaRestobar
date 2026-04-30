package com.laprevia.restobar.data.mapper

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.laprevia.restobar.data.local.entity.OrderEntity
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.data.model.OrderStatus

private val gson = Gson()

// ROOM → DOMAIN
fun OrderEntity.toDomain(): Order {
    val items = try {
        val type = object : TypeToken<List<OrderItem>>() {}.type
        val result: List<OrderItem> = gson.fromJson(itemsJson, type)
        result ?: emptyList()
    } catch (e: Exception) {
        println("❌ Error al convertir itemsJson: ${e.message}")
        emptyList()
    }

    return Order(
        id = id,
        tableId = tableId,
        tableNumber = tableNumber,
        items = items,
        status = OrderStatus.valueOf(status),
        createdAt = createdAt,
        updatedAt = updatedAt,
        total = total,
        waiterId = waiterId,
        waiterName = waiterName,
        notes = notes
    )
}

// DOMAIN → ROOM
fun Order.toEntity(): OrderEntity {
    val itemsJson: String = gson.toJson(items, object : TypeToken<List<OrderItem>>() {}.type)

    return OrderEntity(
        id = id,
        tableId = tableId,
        tableNumber = tableNumber,
        status = status.name,
        total = total,
        createdAt = createdAt,
        updatedAt = updatedAt,
        waiterId = waiterId,
        waiterName = waiterName,
        notes = notes,
        itemsJson = itemsJson,
        syncStatus = "PENDING",
        version = System.currentTimeMillis(),
        lastModified = System.currentTimeMillis()
    )
}
package com.laprevia.restobar.domain

import com.laprevia.restobar.data.model.OrderItem

data class StockShortage(
    val productId: String,
    val productName: String,
    val requested: Int,
    val available: Double
)

object OrderStockValidation {
    fun shortages(
        items: List<OrderItem>,
        availableStock: Map<String, Double>
    ): List<StockShortage> = items
        .filter { it.trackInventory }
        .groupBy { it.productId }
        .mapNotNull { (productId, productItems) ->
            val requested = productItems.sumOf { it.quantity }
            val available = availableStock[productId]
                ?.takeIf { it.isFinite() && it >= 0.0 }
                ?: 0.0
            if (requested > available) {
                StockShortage(
                    productId = productId,
                    productName = productItems.first().productName,
                    requested = requested,
                    available = available
                )
            } else {
                null
            }
        }
}

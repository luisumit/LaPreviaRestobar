package com.laprevia.restobar.data.model

import java.util.*

data class Order(
    val id: String = UUID.randomUUID().toString(),
    val tableId: Int = 0,
    val tableNumber: Int = 0,
    val items: List<OrderItem> = emptyList(),
    val status: OrderStatus = OrderStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val total: Double = 0.0,
    val waiterId: String? = null,
    val waiterName: String? = null,
    val notes: String? = null,
    val version: Long = 0,
    val syncStatus: String = "PENDING"
) {
    constructor() : this(
        id = "",
        tableId = 0,
        tableNumber = 0,
        items = emptyList(),
        status = OrderStatus.PENDING,
        createdAt = 0,
        updatedAt = 0,
        total = 0.0
    )

    fun calculateTotal(): Double = items.sumOf { it.subtotal }
    fun isValid(): Boolean = id.isNotBlank() && tableNumber > 0
    fun getValidItems(): List<OrderItem> = items.filter { it.isValid() }
}

// ✅ OrderItem con campos planos para Firebase
data class OrderItem(
    val productId: String = "",
    val productName: String = "",
    val productDescription: String = "",
    val productPrice: Double = 0.0,
    val productCategory: String = "",
    val trackInventory: Boolean = false,
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val subtotal: Double = 0.0
) {
    constructor() : this(
        productId = "",
        productName = "",
        productDescription = "",
        productPrice = 0.0,
        productCategory = "",
        trackInventory = false,
        quantity = 0,
        unitPrice = 0.0,
        subtotal = 0.0
    )

    constructor(product: Product, quantity: Int) : this(
        productId = product.id,
        productName = product.name,
        productDescription = product.description,
        productPrice = product.salePrice ?: 0.0,
        productCategory = product.category,
        trackInventory = product.trackInventory,
        quantity = quantity,
        unitPrice = product.salePrice ?: 0.0,
        subtotal = (product.salePrice ?: 0.0) * quantity
    )

    fun isValid(): Boolean = productId.isNotBlank() && productName.isNotBlank() && quantity > 0
    fun toProduct(): Product = Product(
        id = productId,
        name = productName,
        description = productDescription,
        category = productCategory,
        salePrice = productPrice,
        trackInventory = trackInventory
    )
}

// ✅ OrderStatus ACTUALIZADO - SOLO SE AGREGÓ ENTREGADO
enum class OrderStatus {
    PENDING,
    ENVIADO,
    ACEPTADO,
    EN_PREPARACION,
    LISTO,
    ENTREGADO,      // ✅ NUEVO: Comida entregada, mesa ocupada
    COMPLETED,
    CANCELLED;

    companion object {
        fun fromString(status: String): OrderStatus {
            return when (status.uppercase()) {
                "PENDING", "PENDIENTE" -> PENDING
                "ENVIADO", "ENVIADA" -> ENVIADO
                "ACEPTADO", "ACEPTADA", "CONFIRMADO" -> ACEPTADO
                "EN_PREPARACION", "PREPARACION", "PREPARANDO" -> EN_PREPARACION
                "LISTO", "LISTA", "READY" -> LISTO
                "ENTREGADO", "ENTREGADA", "DELIVERED" -> ENTREGADO  // ✅ NUEVO
                "COMPLETED", "COMPLETADA", "TERMINADO" -> COMPLETED
                "CANCELLED", "CANCELADA", "CANCELADO" -> CANCELLED
                else -> PENDING
            }
        }

        fun valueOfOrNull(status: String): OrderStatus? {
            return try {
                valueOf(status.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}

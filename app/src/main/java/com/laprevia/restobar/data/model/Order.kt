// Order.kt - VERSIÓN CORREGIDA CON CAMPOS PLANOS PARA FIREBASE
package com.laprevia.restobar.data.model

import java.util.*

data class Order(
    val id: String = UUID.randomUUID().toString(),
    val tableId: Int = 0,
    val tableNumber: Int = 0,
    val items: List<OrderItem> = emptyList(),
    val status: OrderStatus = OrderStatus.ENVIADO,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val total: Double = 0.0,
    val waiterId: String? = null,
    val waiterName: String? = null,
    val notes: String? = null
) {
    // CONSTRUCTOR SIN ARGUMENTOS PARA FIREBASE
    constructor() : this(
        id = "",
        tableId = 0,
        tableNumber = 0,
        items = emptyList(),
        status = OrderStatus.ENVIADO,
        createdAt = 0,
        updatedAt = 0,
        total = 0.0
    )

    // Método para calcular total
    fun calculateTotal(): Double {
        return items.sumOf { it.subtotal }
    }

    // Método para verificar si la orden es válida
    fun isValid(): Boolean {
        return id.isNotBlank() && tableNumber > 0
    }

    // Método para obtener items válidos
    fun getValidItems(): List<OrderItem> {
        return items.filter { it.isValid() }
    }
}

// ✅ VERSIÓN CORREGIDA: OrderItem con campos planos
data class OrderItem(
    // ✅ CAMPOS PLANOS DEL PRODUCTO (no objeto anidado)
    val productId: String = "",
    val productName: String = "",
    val productDescription: String = "",
    val productPrice: Double = 0.0,
    val productCategory: String = "",
    val trackInventory: Boolean = false,

    // Datos del item
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val subtotal: Double = 0.0
) {
    // CONSTRUCTOR SIN ARGUMENTOS PARA FIREBASE
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

    // ✅ CONSTRUCTOR DESDE PRODUCT (para el mesero)
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

    // Método para verificar si el item es válido
    fun isValid(): Boolean {
        return productId.isNotBlank() && productName.isNotBlank() && quantity > 0
    }

    // Método para crear un Product simplificado (para compatibilidad)
    fun toProduct(): Product {
        return Product(
            id = productId,
            name = productName,
            description = productDescription,
            category = productCategory,
            salePrice = productPrice,
            trackInventory = trackInventory
        )
    }
}

// OrderStatus está bien
enum class OrderStatus {
    ENVIADO, ACEPTADO, EN_PREPARACION, LISTO, COMPLETED, CANCELLED;

    companion object {
        fun fromString(status: String): OrderStatus {
            return when (status.uppercase()) {
                "PENDIENTE", "ENVIADO" -> ENVIADO
                "CONFIRMADO", "ACEPTADO" -> ACEPTADO
                "PREPARACION", "EN_PREPARACION" -> EN_PREPARACION
                "LISTO", "TERMINADO" -> LISTO
                else -> ENVIADO
            }
        }
    }
}
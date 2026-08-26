package com.laprevia.restobar.data.model

import com.laprevia.restobar.domain.model.Money
import com.laprevia.restobar.platform.currentTimeMillis
import com.laprevia.restobar.platform.randomUuid

data class Order(
    val id: String = randomUuid(),
    val tableId: Int = 0,
    val tableNumber: Int = 0,
    val items: List<OrderItem> = emptyList(),
    val status: OrderStatus = OrderStatus.PENDING,
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis(),
    val total: Double = 0.0,
    val waiterId: String? = null,
    val waiterName: String? = null,
    val notes: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.UNSPECIFIED,
    val paidAt: Long? = null,
    val receiptNumber: String? = null,
    val amountReceived: Double? = null,
    val changeGiven: Double? = null,
    val discountAmount: Double? = null,
    val discountReason: String? = null,
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

    // ==================== Comportamiento de Aggregate Root (DDD) ====================
    // El pedido es la raiz del agregado: toda modificacion de estado pasa por aqui,
    // garantizando las invariantes del negocio. Devuelve copias (inmutabilidad).

    /** Subtotal calculado desde los items (antes de descuento). */
    fun subtotal(): Money = Money(items.sumOf { item ->
        item.subtotal.takeIf { it > 0.0 } ?: (item.unitPrice * item.quantity)
    })

    /** Total a cobrar: el total guardado (ya neto), o el subtotal si aun no hay total. */
    fun grandTotal(): Money = if (total > 0.0) Money(total) else subtotal()

    fun hasItems(): Boolean = items.any { it.isValid() }

    /** Invariante: solo se cobra un pedido con items y que no este ya cerrado o cancelado. */
    fun canBeCharged(): Boolean =
        hasItems() && status != OrderStatus.CANCELLED && status != OrderStatus.COMPLETED

    /**
     * Aplica un descuento. Invariante: el total nunca queda negativo y el descuento
     * no puede superar al total. Devuelve una copia del agregado.
     */
    fun applyDiscount(discount: Money, reason: String): Order {
        val capped = discount.atLeastZero().let { if (it > grandTotal()) grandTotal() else it }
        val net = (grandTotal() - capped).atLeastZero()
        return copy(
            total = net.amount,
            discountAmount = capped.amount.takeIf { it > 0.0 },
            discountReason = reason.takeIf { capped.isPositive() && it.isNotBlank() }
        )
    }

    /** Vuelto para un monto recibido (nunca negativo). */
    fun changeFor(received: Money): Money = (received - grandTotal()).atLeastZero()

    /**
     * Registra el pago. Invariante: el vuelto solo aplica a pagos en efectivo.
     * Devuelve una copia con metodo, recibido y vuelto.
     */
    fun payWith(method: PaymentMethod, received: Money? = null): Order {
        val cashReceived = received?.takeIf { method == PaymentMethod.CASH && it.isPositive() }
        val change = cashReceived?.let { changeFor(it) }
        return copy(
            paymentMethod = method,
            amountReceived = cashReceived?.amount,
            changeGiven = change?.amount
        )
    }
}

enum class PaymentMethod(val label: String) {
    UNSPECIFIED("Sin especificar"),
    CASH("Efectivo"),
    YAPE_PLIN("Yape/Plin"),
    CARD("Tarjeta");

    companion object {
        fun fromString(value: String?): PaymentMethod {
            return entries.find { it.name == value } ?: UNSPECIFIED
        }
    }
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

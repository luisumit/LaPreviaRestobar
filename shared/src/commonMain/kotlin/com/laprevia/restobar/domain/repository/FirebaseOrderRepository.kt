package com.laprevia.restobar.domain.repository

import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.platform.currentTimeMillis
import kotlinx.coroutines.flow.Flow

interface FirebaseOrderRepository : OrderRepository {

    /**
     * Actualiza SOLO los items y el total de un pedido (sin tocar el status),
     * para que editar un pedido activo no revierta un cambio de estado hecho por la
     * cocina. La implementacion por defecto reutiliza getOrderById + updateOrder
     * preservando el status fresco; el adaptador de Android lo sobrescribe con un
     * update parcial mas eficiente.
     */
    suspend fun updateOrderItems(orderId: String, items: List<OrderItem>, total: Double) {
        val fresh = getOrderById(orderId) ?: return
        updateOrder(fresh.copy(items = items, total = total, updatedAt = currentTimeMillis()))
    }

    // Métodos específicos para Firebase (tiempo real)
    fun listenToNewOrders(): Flow<Order>
    fun listenToOrderChanges(): Flow<Order>
    fun getOrdersRealTime(): Flow<List<Order>>
    fun getActiveOrders(): Flow<List<Order>>

    // ✅ MÉTODO AGREGADO: Obtener lista de órdenes (suspending)
    suspend fun getOrdersList(): List<Order>

    // ✅ Sube el metodo de pago a Firebase al cobrar (no solo el status)
    suspend fun updateOrderPayment(
        orderId: String,
        status: String,
        paymentMethod: String,
        paidAt: Long,
        receiptNumber: String,
        amountReceived: Double,
        changeGiven: Double
    )
}
package com.laprevia.restobar.domain.repository

import com.laprevia.restobar.data.model.Order
import kotlinx.coroutines.flow.Flow

interface FirebaseOrderRepository : OrderRepository {
    // Métodos específicos para Firebase (tiempo real)
    fun listenToNewOrders(): Flow<Order>
    fun listenToOrderChanges(): Flow<Order>
    fun getOrdersRealTime(): Flow<List<Order>>
    fun getActiveOrders(): Flow<List<Order>>

    // ✅ MÉTODO AGREGADO: Obtener lista de órdenes (suspending)
    suspend fun getOrdersList(): List<Order>
}
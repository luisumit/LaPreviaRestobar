// domain/repository/OrderRepository.kt
package com.laprevia.restobar.domain.repository

import com.laprevia.restobar.data.model.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun getOrders(): Flow<List<Order>>
    fun getOrdersWithItems(): Flow<List<Order>>
    fun getActiveOrdersWithItems(): Flow<List<Order>>
    suspend fun createOrder(order: Order)
    suspend fun updateOrderStatus(orderId: String, status: String)
    suspend fun getOrderById(orderId: String): Order?
    suspend fun getOrdersByTable(tableId: Int): List<Order>
    suspend fun deleteOrder(orderId: String)
    suspend fun updateOrder(order: Order)
    fun getOrdersByStatus(status: String): Flow<List<Order>>
}
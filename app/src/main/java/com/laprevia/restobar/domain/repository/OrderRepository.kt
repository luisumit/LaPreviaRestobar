package com.laprevia.restobar.domain.repository

import com.laprevia.restobar.data.model.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {

    // ==================== LISTADOS ====================
    fun getOrders(): Flow<List<Order>>
    fun getOrdersWithItems(): Flow<List<Order>>
    fun getActiveOrdersWithItems(): Flow<List<Order>>
    fun getOrdersByStatus(status: String): Flow<List<Order>>

    // ==================== BÚSQUEDA ====================
    suspend fun getOrderById(orderId: String): Order?
    suspend fun getOrdersByTable(tableId: Int): List<Order>

    // ==================== CRUD ====================
    suspend fun createOrder(order: Order)
    suspend fun updateOrder(order: Order)
    suspend fun deleteOrder(orderId: String)

    // ==================== ESTADO ====================
    suspend fun updateOrderStatus(orderId: String, status: String)

    // ==================== OFFLINE SYNC (IMPORTANTE) ====================

    /**
     * Marca órdenes pendientes para sincronizar con Firebase
     */
    suspend fun syncPendingOrders()

    /**
     * Obtiene órdenes no sincronizadas (Room)
     */
    fun getPendingOrders(): Flow<List<Order>>
}
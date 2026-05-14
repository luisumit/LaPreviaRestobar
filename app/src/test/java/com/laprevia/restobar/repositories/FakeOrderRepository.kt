package com.laprevia.restobar.repositories

import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.domain.repository.FirebaseOrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeOrderRepository : FirebaseOrderRepository {
    private val orders = mutableListOf<Order>()

    // ==================== FirebaseOrderRepository ====================
    override fun listenToNewOrders(): Flow<Order> = flowOf()
    override fun listenToOrderChanges(): Flow<Order> = flowOf()
    override fun getOrdersRealTime(): Flow<List<Order>> = flowOf(orders)
    override fun getActiveOrders(): Flow<List<Order>> = flowOf(orders.filter {
        it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED
    })

    // ==================== OrderRepository ====================
    override fun getOrders(): Flow<List<Order>> = flowOf(orders)
    override fun getOrdersWithItems(): Flow<List<Order>> = flowOf(orders)
    override fun getActiveOrdersWithItems(): Flow<List<Order>> = flowOf(orders)
    override fun getOrdersByStatus(status: String): Flow<List<Order>> = flowOf(orders.filter { it.status.name == status })

    override suspend fun getOrderById(orderId: String): Order? = orders.find { it.id == orderId }
    override suspend fun getOrdersByTable(tableId: Int): List<Order> = orders.filter { it.tableId == tableId }
    override suspend fun getOrdersList(): List<Order> = orders  // ✅ agregado

    override suspend fun createOrder(order: Order) { orders.add(order) }
    override suspend fun updateOrder(order: Order) {
        val index = orders.indexOfFirst { it.id == order.id }
        if (index != -1) orders[index] = order
    }
    override suspend fun deleteOrder(orderId: String) { orders.removeAll { it.id == orderId } }
    override suspend fun updateOrderStatus(orderId: String, status: String) {
        val index = orders.indexOfFirst { it.id == orderId }
        if (index != -1) {
            orders[index] = orders[index].copy(status = OrderStatus.valueOf(status))
        }
    }
    override suspend fun syncPendingOrders() = Unit
    override fun getPendingOrders(): Flow<List<Order>> = flowOf(emptyList())
}
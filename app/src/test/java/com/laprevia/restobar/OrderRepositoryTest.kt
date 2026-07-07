package com.laprevia.restobar

import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.repositories.FakeOrderRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderRepositoryTest {

    @Test
    fun `getActiveOrders excluye ordenes completadas y canceladas`() = runTest {
        val repository = FakeOrderRepository()
        repository.createOrder(Order(id = "o1", tableId = 1, tableNumber = 1, status = OrderStatus.ENVIADO))
        repository.createOrder(Order(id = "o2", tableId = 2, tableNumber = 2, status = OrderStatus.COMPLETED))
        repository.createOrder(Order(id = "o3", tableId = 3, tableNumber = 3, status = OrderStatus.CANCELLED))

        val activeOrders = repository.getActiveOrders().first()

        assertEquals(1, activeOrders.size)
        assertEquals("o1", activeOrders.first().id)
    }

    @Test
    fun `updateOrderStatus cambia estado del pedido`() = runTest {
        val repository = FakeOrderRepository()
        repository.createOrder(Order(id = "o1", tableId = 1, tableNumber = 1, status = OrderStatus.ENVIADO))

        repository.updateOrderStatus("o1", "LISTO")

        assertEquals(OrderStatus.LISTO, repository.getOrderById("o1")?.status)
    }

    @Test
    fun `deleteOrder elimina pedido por id`() = runTest {
        val repository = FakeOrderRepository()
        repository.createOrder(Order(id = "o1", tableId = 1, tableNumber = 1))

        repository.deleteOrder("o1")

        assertNull(repository.getOrderById("o1"))
    }

    @Test
    fun `getOrdersByTable filtra pedidos por mesa`() = runTest {
        val repository = FakeOrderRepository()
        repository.createOrder(Order(id = "o1", tableId = 1, tableNumber = 1))
        repository.createOrder(Order(id = "o2", tableId = 2, tableNumber = 2))
        repository.createOrder(Order(id = "o3", tableId = 1, tableNumber = 1))

        val tableOrders = repository.getOrdersByTable(1)

        assertEquals(listOf("o1", "o3"), tableOrders.map { it.id })
    }
}

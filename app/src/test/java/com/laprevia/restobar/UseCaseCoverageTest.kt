package com.laprevia.restobar

import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.data.model.TableStatus
import com.laprevia.restobar.domain.usecase.CreateOrderUseCase
import com.laprevia.restobar.domain.usecase.CreateProductUseCase
import com.laprevia.restobar.domain.usecase.DeleteProductUseCase
import com.laprevia.restobar.domain.usecase.GetProductsUseCase
import com.laprevia.restobar.domain.usecase.GetTablesUseCase
import com.laprevia.restobar.domain.usecase.UpdateOrderStatusUseCase
import com.laprevia.restobar.domain.usecase.UpdateProductUseCase
import com.laprevia.restobar.repositories.FakeOrderRepository
import com.laprevia.restobar.repositories.FakeProductRepository
import com.laprevia.restobar.repositories.FakeTableRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UseCaseCoverageTest {

    @Test
    fun `create order use case registra pedido y ocupa la mesa`() = runTest {
        val orderRepository = FakeOrderRepository()
        val tableRepository = FakeTableRepository()
        tableRepository.initializeDefaultTables()
        val useCase = CreateOrderUseCase(orderRepository, tableRepository)

        useCase(Order(id = "order-10", tableId = 2, tableNumber = 2))

        val storedOrder = orderRepository.getOrderById("order-10")
        val table = tableRepository.getTableById(2)

        assertEquals("order-10", storedOrder?.id)
        assertEquals(TableStatus.OCUPADA, table?.status)
        assertEquals("order-10", table?.currentOrderId)
    }

    @Test
    fun `update order status use case cambia el estado del pedido`() = runTest {
        val repository = FakeOrderRepository()
        repository.createOrder(Order(id = "order-20", tableId = 1, tableNumber = 1))
        val useCase = UpdateOrderStatusUseCase(repository)

        useCase("order-20", OrderStatus.ENTREGADO)

        assertEquals(OrderStatus.ENTREGADO, repository.getOrderById("order-20")?.status)
    }

    @Test
    fun `product use cases crean actualizan listan y eliminan productos`() = runTest {
        val repository = FakeProductRepository()
        val createProduct = CreateProductUseCase(repository)
        val updateProduct = UpdateProductUseCase(repository)
        val deleteProduct = DeleteProductUseCase(repository)
        val getProducts = GetProductsUseCase(repository)

        createProduct(Product(id = "p1", name = "Ceviche", salePrice = 22.0))
        createProduct(Product(id = "p2", name = "Chicha", salePrice = 7.0))
        updateProduct(Product(id = "p2", name = "Chicha morada", salePrice = 8.0))
        deleteProduct("p1")

        val products = getProducts().first()

        assertEquals(1, products.size)
        assertEquals("p2", products.first().id)
        assertEquals("Chicha morada", products.first().name)
        assertEquals(8.0, products.first().salePrice ?: 0.0, 0.001)
    }

    @Test
    fun `get tables use case devuelve mesas inicializadas`() = runTest {
        val repository = FakeTableRepository()
        repository.initializeDefaultTables()
        val useCase = GetTablesUseCase(repository)

        val tables = useCase().first()

        assertEquals(8, tables.size)
        assertEquals(TableStatus.LIBRE, tables.first().status)
        assertNull(tables.first().currentOrderId)
    }
}

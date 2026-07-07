package com.laprevia.restobar

import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderDomainTest {

    @Test
    fun `calculateTotal suma subtotales de los items`() {
        val order = Order(
            id = "order-1",
            tableNumber = 3,
            items = listOf(
                OrderItem(productId = "p1", productName = "Ceviche", quantity = 2, unitPrice = 18.0, subtotal = 36.0),
                OrderItem(productId = "p2", productName = "Chicha", quantity = 1, unitPrice = 7.5, subtotal = 7.5)
            )
        )

        assertEquals(43.5, order.calculateTotal(), 0.001)
    }

    @Test
    fun `isValid requiere id y numero de mesa valido`() {
        assertTrue(Order(id = "order-1", tableNumber = 1).isValid())
        assertFalse(Order(id = "", tableNumber = 1).isValid())
        assertFalse(Order(id = "order-1", tableNumber = 0).isValid())
    }

    @Test
    fun `getValidItems devuelve solo items completos`() {
        val order = Order(
            id = "order-1",
            tableNumber = 1,
            items = listOf(
                OrderItem(productId = "p1", productName = "Lomo", quantity = 1),
                OrderItem(productId = "", productName = "Sin id", quantity = 1),
                OrderItem(productId = "p2", productName = "Sin cantidad", quantity = 0)
            )
        )

        val validItems = order.getValidItems()

        assertEquals(1, validItems.size)
        assertEquals("p1", validItems.first().productId)
    }

    @Test
    fun `OrderItem creado desde producto calcula precio y subtotal`() {
        val product = Product(
            id = "p1",
            name = "Anticucho",
            description = "Porcion",
            category = "Comidas",
            salePrice = 12.0,
            trackInventory = true
        )

        val item = OrderItem(product, quantity = 3)

        assertEquals("p1", item.productId)
        assertEquals("Anticucho", item.productName)
        assertEquals(12.0, item.unitPrice, 0.001)
        assertEquals(36.0, item.subtotal, 0.001)
        assertTrue(item.trackInventory)
    }

    @Test
    fun `OrderStatus convierte textos usados por la app`() {
        assertEquals(OrderStatus.PENDING, OrderStatus.fromString("pendiente"))
        assertEquals(OrderStatus.ENVIADO, OrderStatus.fromString("enviada"))
        assertEquals(OrderStatus.ACEPTADO, OrderStatus.fromString("confirmado"))
        assertEquals(OrderStatus.EN_PREPARACION, OrderStatus.fromString("preparando"))
        assertEquals(OrderStatus.LISTO, OrderStatus.fromString("ready"))
        assertEquals(OrderStatus.ENTREGADO, OrderStatus.fromString("delivered"))
        assertEquals(OrderStatus.COMPLETED, OrderStatus.fromString("terminado"))
        assertEquals(OrderStatus.CANCELLED, OrderStatus.fromString("cancelado"))
    }

    @Test
    fun `valueOfOrNull devuelve null con estado desconocido`() {
        assertEquals(OrderStatus.LISTO, OrderStatus.valueOfOrNull("LISTO"))
        assertNull(OrderStatus.valueOfOrNull("NO_EXISTE"))
    }
}

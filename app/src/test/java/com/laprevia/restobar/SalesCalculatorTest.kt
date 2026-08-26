package com.laprevia.restobar

import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.data.model.PaymentMethod
import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.service.SalesCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.util.Calendar
import org.junit.Test

/**
 * Pruebas de las agregaciones de reportes/cierre de caja: totales, metodos de pago,
 * ganancia bruta, top productos y ventas por hora.
 */
class SalesCalculatorTest {

    private val delta = 0.001

    private fun item(productId: String, name: String, qty: Int, price: Double) =
        OrderItem(productId = productId, productName = name, quantity = qty, unitPrice = price, subtotal = price * qty)

    private fun order(
        total: Double,
        method: PaymentMethod = PaymentMethod.CASH,
        items: List<OrderItem> = emptyList(),
        paidAt: Long? = null
    ) = Order(id = "o", tableNumber = 1, items = items, total = total, paymentMethod = method, paidAt = paidAt)

    private fun timestampAtHour(hour: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

    // ---------- orderTotal ----------

    @Test
    fun `orderTotal usa el total guardado`() {
        assertEquals(50.0, SalesCalculator.orderTotal(order(total = 50.0)), delta)
    }

    @Test
    fun `orderTotal cae a la suma de items si el total es cero`() {
        val o = order(total = 0.0, items = listOf(item("p", "Cerveza", 2, 10.0)))
        assertEquals(20.0, SalesCalculator.orderTotal(o), delta)
    }

    // ---------- paymentTotal ----------

    @Test
    fun `paymentTotal suma solo el metodo pedido`() {
        val orders = listOf(
            order(total = 30.0, method = PaymentMethod.CASH),
            order(total = 20.0, method = PaymentMethod.CASH),
            order(total = 15.0, method = PaymentMethod.YAPE_PLIN)
        )
        assertEquals(50.0, SalesCalculator.paymentTotal(orders, PaymentMethod.CASH), delta)
        assertEquals(15.0, SalesCalculator.paymentTotal(orders, PaymentMethod.YAPE_PLIN), delta)
        assertEquals(0.0, SalesCalculator.paymentTotal(orders, PaymentMethod.CARD), delta)
    }

    // ---------- grossProfit ----------

    @Test
    fun `grossProfit resta el costo del producto`() {
        val products = listOf(Product(id = "p1", name = "Cerveza", costPrice = 6.0, salePrice = 10.0))
        val orders = listOf(order(total = 20.0, items = listOf(item("p1", "Cerveza", 2, 10.0))))
        // venta 20 - costo (6*2=12) = 8
        assertEquals(8.0, SalesCalculator.grossProfit(orders, products), delta)
    }

    @Test
    fun `grossProfit sin costo conocido es igual a la venta`() {
        val orders = listOf(order(total = 20.0, items = listOf(item("desconocido", "X", 1, 20.0))))
        assertEquals(20.0, SalesCalculator.grossProfit(orders, emptyList()), delta)
    }

    // ---------- productsSold ----------

    @Test
    fun `productsSold suma las cantidades`() {
        val orders = listOf(
            order(total = 30.0, items = listOf(item("p1", "A", 2, 10.0), item("p2", "B", 1, 10.0))),
            order(total = 10.0, items = listOf(item("p1", "A", 3, 10.0)))
        )
        assertEquals(6, SalesCalculator.productsSold(orders))
    }

    // ---------- topProducts ----------

    @Test
    fun `topProducts ordena por cantidad y respeta el limite`() {
        val orders = listOf(
            order(total = 0.0, items = listOf(item("p1", "Cerveza", 5, 10.0))),
            order(total = 0.0, items = listOf(item("p2", "Pisco", 2, 15.0))),
            order(total = 0.0, items = listOf(item("p3", "Agua", 8, 3.0)))
        )
        val top = SalesCalculator.topProducts(orders, limit = 2)
        assertEquals(2, top.size)
        assertEquals("Agua", top[0].name)     // 8 unidades
        assertEquals("Cerveza", top[1].name)  // 5 unidades
        assertEquals(8, top[0].quantity)
    }

    // ---------- salesByHour ----------

    @Test
    fun `salesByHour agrupa por hora del dia`() {
        val orders = listOf(
            order(total = 30.0, paidAt = timestampAtHour(14)),
            order(total = 20.0, paidAt = timestampAtHour(14)),
            order(total = 40.0, paidAt = timestampAtHour(20))
        )
        val byHour = SalesCalculator.salesByHour(orders)
        val h14 = byHour.find { it.label == "14h" }
        val h20 = byHour.find { it.label == "20h" }
        assertTrue(h14 != null && h20 != null)
        assertEquals(50.0, h14!!.total, delta)
        assertEquals(40.0, h20!!.total, delta)
    }
}

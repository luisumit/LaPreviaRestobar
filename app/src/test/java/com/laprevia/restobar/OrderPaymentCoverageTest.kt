package com.laprevia.restobar

import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.PaymentMethod
import com.laprevia.restobar.data.model.UserRole
import com.laprevia.restobar.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderPaymentCoverageTest {

    @Test
    fun `pedido con items validos puede cobrarse si no esta cerrado`() {
        val order = Order(
            id = "order-1",
            tableNumber = 4,
            status = OrderStatus.ENTREGADO,
            items = listOf(OrderItem(productId = "p1", productName = "Lomo", quantity = 2, unitPrice = 18.0))
        )

        assertTrue(order.hasItems())
        assertTrue(order.canBeCharged())
    }

    @Test
    fun `pedido completado o cancelado no puede cobrarse de nuevo`() {
        val item = OrderItem(productId = "p1", productName = "Lomo", quantity = 1, unitPrice = 18.0)

        assertFalse(Order(id = "o1", tableNumber = 1, status = OrderStatus.COMPLETED, items = listOf(item)).canBeCharged())
        assertFalse(Order(id = "o2", tableNumber = 1, status = OrderStatus.CANCELLED, items = listOf(item)).canBeCharged())
    }

    @Test
    fun `discount aplica tope y limpia razon si descuento es cero`() {
        val order = Order(id = "order-2", tableNumber = 1, total = 40.0)

        val discounted = order.applyDiscount(Money(100.0), "Promocion")
        val withoutDiscount = order.applyDiscount(Money(-10.0), "No aplica")

        assertEquals(0.0, discounted.total, 0.001)
        assertEquals(40.0, discounted.discountAmount ?: 0.0, 0.001)
        assertEquals("Promocion", discounted.discountReason)
        assertEquals(40.0, withoutDiscount.total, 0.001)
        assertNull(withoutDiscount.discountAmount)
        assertNull(withoutDiscount.discountReason)
    }

    @Test
    fun `pago en efectivo guarda recibido y vuelto`() {
        val order = Order(id = "order-3", tableNumber = 1, total = 56.0)

        val paid = order.payWith(PaymentMethod.CASH, Money(60.0))

        assertEquals(PaymentMethod.CASH, paid.paymentMethod)
        assertEquals(60.0, paid.amountReceived ?: 0.0, 0.001)
        assertEquals(4.0, paid.changeGiven ?: 0.0, 0.001)
    }

    @Test
    fun `pago con tarjeta o yape no guarda vuelto de efectivo`() {
        val order = Order(id = "order-4", tableNumber = 1, total = 20.0)

        val cardPayment = order.payWith(PaymentMethod.CARD, Money(50.0))
        val yapePayment = order.payWith(PaymentMethod.YAPE_PLIN, Money(50.0))

        assertEquals(PaymentMethod.CARD, cardPayment.paymentMethod)
        assertNull(cardPayment.amountReceived)
        assertNull(cardPayment.changeGiven)
        assertEquals(PaymentMethod.YAPE_PLIN, yapePayment.paymentMethod)
        assertNull(yapePayment.amountReceived)
        assertNull(yapePayment.changeGiven)
    }

    @Test
    fun `payment method y user role convierten textos conocidos y desconocidos`() {
        assertEquals(PaymentMethod.CASH, PaymentMethod.fromString("CASH"))
        assertEquals(PaymentMethod.YAPE_PLIN, PaymentMethod.fromString("YAPE_PLIN"))
        assertEquals(PaymentMethod.CARD, PaymentMethod.fromString("CARD"))
        assertEquals(PaymentMethod.UNSPECIFIED, PaymentMethod.fromString(null))
        assertEquals(PaymentMethod.UNSPECIFIED, PaymentMethod.fromString("OTRO"))

        assertEquals("ADMIN", UserRole.fromString("admin").name)
        assertEquals("COCINERO", UserRole.fromString("cocinero").name)
        assertEquals("MESERO", UserRole.fromString("no-existe").name)
    }
}

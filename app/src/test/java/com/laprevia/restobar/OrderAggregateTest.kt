package com.laprevia.restobar

import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.PaymentMethod
import com.laprevia.restobar.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del Aggregate Root `Order`: comportamiento e invariantes del negocio.
 */
class OrderAggregateTest {

    private val delta = 0.001

    private fun item(name: String, qty: Int, price: Double) =
        OrderItem(productId = name, productName = name, quantity = qty, unitPrice = price, subtotal = price * qty)

    private fun order(
        total: Double = 0.0,
        status: OrderStatus = OrderStatus.LISTO,
        items: List<OrderItem> = listOf(item("Cerveza", 2, 10.0))
    ) = Order(id = "o1", tableId = 1, tableNumber = 1, items = items, status = status, total = total)

    // ---------- Totales ----------

    @Test
    fun `subtotal suma los items`() {
        assertEquals(20.0, order().subtotal().amount, delta)
    }

    @Test
    fun `grandTotal usa el total guardado si existe`() {
        assertEquals(50.0, order(total = 50.0).grandTotal().amount, delta)
    }

    // ---------- Invariante: puede cobrarse ----------

    @Test
    fun `un pedido con items y abierto puede cobrarse`() {
        assertTrue(order(status = OrderStatus.LISTO).canBeCharged())
    }

    @Test
    fun `un pedido sin items NO puede cobrarse`() {
        assertFalse(order(items = emptyList()).canBeCharged())
    }

    @Test
    fun `un pedido cancelado NO puede cobrarse`() {
        assertFalse(order(status = OrderStatus.CANCELLED).canBeCharged())
    }

    // ---------- Descuento (invariante: no negativo) ----------

    @Test
    fun `aplicar descuento reduce el total y guarda el motivo`() {
        val result = order(total = 50.0).applyDiscount(Money(10.0), "Happy Hour")
        assertEquals(40.0, result.total, delta)
        assertEquals(10.0, result.discountAmount!!, delta)
        assertEquals("Happy Hour", result.discountReason)
    }

    @Test
    fun `descuento mayor al total no deja el neto negativo`() {
        val result = order(total = 50.0).applyDiscount(Money(80.0), "Error")
        assertTrue("El total nunca es negativo", result.total >= 0.0)
        assertEquals(0.0, result.total, delta)
    }

    @Test
    fun `descuento cero no marca descuento`() {
        val result = order(total = 50.0).applyDiscount(Money(0.0), "Ninguno")
        assertEquals(50.0, result.total, delta)
        assertNull(result.discountAmount)
        assertNull(result.discountReason)
    }

    // ---------- Pago y vuelto (invariante: vuelto solo en efectivo) ----------

    @Test
    fun `pago en efectivo calcula el vuelto`() {
        val result = order(total = 30.0).payWith(PaymentMethod.CASH, Money(50.0))
        assertEquals(PaymentMethod.CASH, result.paymentMethod)
        assertEquals(50.0, result.amountReceived!!, delta)
        assertEquals(20.0, result.changeGiven!!, delta)
    }

    @Test
    fun `pago con tarjeta no lleva vuelto`() {
        val result = order(total = 30.0).payWith(PaymentMethod.CARD, Money(50.0))
        assertEquals(PaymentMethod.CARD, result.paymentMethod)
        assertNull(result.amountReceived)
        assertNull(result.changeGiven)
    }

    // ---------- Flujo completo por el agregado ----------

    @Test
    fun `flujo cobro con descuento y efectivo`() {
        val cobrado = order(total = 100.0)
            .applyDiscount(Money(20.0), "Descuento 20%")
            .payWith(PaymentMethod.CASH, Money(100.0))

        assertEquals(80.0, cobrado.total, delta)        // neto
        assertEquals(20.0, cobrado.discountAmount!!, delta)
        assertEquals(20.0, cobrado.changeGiven!!, delta) // 100 - 80
    }
}

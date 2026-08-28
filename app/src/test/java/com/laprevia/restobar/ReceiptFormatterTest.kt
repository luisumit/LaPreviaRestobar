package com.laprevia.restobar

import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.data.model.PaymentMethod
import com.laprevia.restobar.data.printer.ReceiptDocument
import com.laprevia.restobar.data.printer.ReceiptFormatter
import com.laprevia.restobar.data.printer.ReceiptLine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del armado de comanda y ticket: formato de mesa (M01), descuento y vuelto.
 */
class ReceiptFormatterTest {

    private val formatter = ReceiptFormatter()

    /** Aplana el documento a un solo texto para poder buscar contenido. */
    private fun ReceiptDocument.render(): String = lines.joinToString(" ") { line ->
        when (line) {
            is ReceiptLine.Text -> line.text
            is ReceiptLine.TwoCols -> "${line.left} ${line.right}"
            is ReceiptLine.Divider -> "----"
            is ReceiptLine.Feed -> ""
        }
    }

    private fun baseOrder(
        tableNumber: Int = 1,
        total: Double = 20.0,
        items: List<OrderItem> = listOf(OrderItem(productName = "Cerveza", quantity = 2, unitPrice = 10.0, subtotal = 20.0)),
        paymentMethod: PaymentMethod = PaymentMethod.CASH,
        amountReceived: Double? = null,
        changeGiven: Double? = null,
        discountAmount: Double? = null,
        discountReason: String? = null
    ) = Order(
        id = "t1",
        tableId = tableNumber,
        tableNumber = tableNumber,
        items = items,
        total = total,
        paymentMethod = paymentMethod,
        receiptNumber = "LP-TEST",
        amountReceived = amountReceived,
        changeGiven = changeGiven,
        discountAmount = discountAmount,
        discountReason = discountReason
    )

    @Test
    fun `comanda usa el formato de mesa M01`() {
        val text = formatter.kitchenComanda(baseOrder(tableNumber = 1)).render()
        assertTrue("Deberia mostrar M01", text.contains("M01"))
    }

    @Test
    fun `ticket usa formato M05 para mesa 5`() {
        val text = formatter.customerTicket(baseOrder(tableNumber = 5)).render()
        assertTrue(text.contains("M05"))
    }

    @Test
    fun `ticket en efectivo muestra recibido y vuelto`() {
        val order = baseOrder(
            total = 20.0,
            paymentMethod = PaymentMethod.CASH,
            amountReceived = 50.0,
            changeGiven = 30.0
        )
        val text = formatter.customerTicket(order).render()
        assertTrue("Debe mostrar Recibido", text.contains("Recibido"))
        assertTrue("Debe mostrar 50.00", text.contains("50.00"))
        assertTrue("Debe mostrar Vuelto", text.contains("Vuelto"))
        assertTrue("Debe mostrar 30.00", text.contains("30.00"))
    }

    @Test
    fun `ticket con descuento muestra subtotal y motivo`() {
        val order = baseOrder(
            total = 53.20, // neto
            items = listOf(OrderItem(productName = "Combo", quantity = 1, unitPrice = 66.5, subtotal = 66.5)),
            discountAmount = 13.30,
            discountReason = "Happy Hour"
        )
        val text = formatter.customerTicket(order).render()
        assertTrue("Debe mostrar Subtotal", text.contains("Subtotal"))
        assertTrue("Debe mostrar el subtotal 66.50", text.contains("66.50"))
        assertTrue("Debe mostrar el motivo", text.contains("Happy Hour"))
        assertTrue("Debe mostrar el descuento", text.contains("13.30"))
        assertTrue("Debe mostrar el TOTAL neto", text.contains("53.20"))
    }

    @Test
    fun `ticket sin descuento no muestra linea de subtotal`() {
        val order = baseOrder(paymentMethod = PaymentMethod.YAPE_PLIN)
        val text = formatter.customerTicket(order).render()
        assertFalse("No debe mostrar Subtotal si no hay descuento", text.contains("Subtotal"))
    }

    @Test
    fun `ticket sin efectivo no muestra vuelto`() {
        val order = baseOrder(paymentMethod = PaymentMethod.CARD)
        val text = formatter.customerTicket(order).render()
        assertFalse(text.contains("Vuelto"))
    }
}

package com.laprevia.restobar

import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.data.model.PaymentMethod
import com.laprevia.restobar.data.printer.LineAlign
import com.laprevia.restobar.data.printer.PaperWidth
import com.laprevia.restobar.data.printer.PrinterConfig
import com.laprevia.restobar.data.printer.ReceiptDocument
import com.laprevia.restobar.data.printer.ReceiptFormatter
import com.laprevia.restobar.data.printer.ReceiptLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptAndPrinterCoverageTest {

    private val formatter = ReceiptFormatter()

    private fun ReceiptDocument.render(): String = lines.joinToString(" ") { line ->
        when (line) {
            is ReceiptLine.Text -> line.text
            is ReceiptLine.TwoCols -> "${line.left} ${line.right}"
            is ReceiptLine.Divider -> "----"
            is ReceiptLine.Feed -> ""
        }
    }

    private fun item(name: String, quantity: Int, price: Double, subtotal: Double = price * quantity) =
        OrderItem(
            productId = name.lowercase(),
            productName = name,
            quantity = quantity,
            unitPrice = price,
            productPrice = price,
            subtotal = subtotal
        )

    @Test
    fun `ticket calcula total desde items si el pedido aun no guarda total`() {
        val order = Order(
            id = "ticket-1",
            tableNumber = 8,
            total = 0.0,
            paymentMethod = PaymentMethod.YAPE_PLIN,
            items = listOf(item("Anticucho", 2, 12.0), item("Gaseosa", 1, 5.0))
        )

        val text = formatter.customerTicket(order).render()

        assertTrue(text.contains("M08"))
        assertTrue(text.contains("Anticucho"))
        assertTrue(text.contains("Gaseosa"))
        assertTrue(text.contains("TOTAL S/ 29.00"))
        assertTrue(text.contains("Pago: Yape/Plin"))
    }

    @Test
    fun `comanda de cocina muestra mesa mozo notas y cantidades sin precios`() {
        val order = Order(
            id = "kitchen-1",
            tableNumber = 3,
            waiterName = "Luis",
            notes = "Sin aji",
            items = listOf(item("Lomo saltado", 1, 18.0), item("Chicha", 2, 6.0))
        )

        val text = formatter.kitchenComanda(order).render()

        assertTrue(text.contains("M03"))
        assertTrue(text.contains("Mozo: Luis"))
        assertTrue(text.contains("Nota: Sin aji"))
        assertTrue(text.contains("1 x Lomo saltado"))
        assertTrue(text.contains("2 x Chicha"))
        assertFalse(text.contains("18.00"))
        assertFalse(text.contains("12.00"))
    }

    @Test
    fun `configuracion de impresora identifica ancho y si esta emparejada`() {
        val configured = PrinterConfig(mac = "00:11:22:AA:BB:CC", name = "Ticketera", paperWidth = PaperWidth.MM_80)
        val empty = PrinterConfig(mac = "", name = null)

        assertEquals(PaperWidth.MM_80, PaperWidth.fromString("MM_80"))
        assertEquals(PaperWidth.MM_58, PaperWidth.fromString(null))
        assertEquals(PaperWidth.MM_58, PaperWidth.fromString("DESCONOCIDO"))
        assertEquals(48, configured.paperWidth.charsPerLine)
        assertTrue(configured.isConfigured)
        assertFalse(empty.isConfigured)
    }

    @Test
    fun `lineas de recibo conservan alineacion y estilos importantes`() {
        val title = ReceiptLine.Text("LA PREVIA RESTOBAR", bold = true, big = true, align = LineAlign.CENTER)
        val total = ReceiptLine.TwoCols("TOTAL", "S/ 48.00", bold = true)
        val feed = ReceiptLine.Feed(3)

        assertEquals(LineAlign.CENTER, title.align)
        assertTrue(title.bold)
        assertTrue(title.big)
        assertEquals("TOTAL", total.left)
        assertEquals("S/ 48.00", total.right)
        assertTrue(total.bold)
        assertEquals(3, feed.lines)
    }
}

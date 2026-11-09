package com.laprevia.restobar

import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.domain.OrderStockValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderStockValidationTest {
    @Test
    fun `stock suficiente permite confirmar`() {
        val items = listOf(item("beer", "Cerveza Artesanal", 3))

        assertTrue(OrderStockValidation.shortages(items, mapOf("beer" to 5.0)).isEmpty())
    }

    @Test
    fun `stock igual a la cantidad solicitada permite confirmar`() {
        val items = listOf(item("sour", "Pisco Sour", 4))

        assertTrue(OrderStockValidation.shortages(items, mapOf("sour" to 4.0)).isEmpty())
    }

    @Test
    fun `reporta producto que supera el stock`() {
        val items = listOf(item("sour", "Pisco Sour", 6))

        val shortage = OrderStockValidation.shortages(items, mapOf("sour" to 4.0)).single()

        assertEquals("Pisco Sour", shortage.productName)
        assertEquals(6, shortage.requested)
        assertEquals(4.0, shortage.available, 0.0)
    }

    @Test
    fun `suma cantidades repetidas y omite productos sin inventario`() {
        val items = listOf(
            item("sour", "Pisco Sour", 3),
            item("sour", "Pisco Sour", 2),
            item("water", "Agua", 8, trackInventory = false)
        )

        val shortage = OrderStockValidation.shortages(items, mapOf("sour" to 4.0)).single()

        assertEquals(5, shortage.requested)
    }

    @Test
    fun `stock faltante o invalido se considera cero`() {
        val items = listOf(item("missing", "Producto sin registro", 1))

        val shortage = OrderStockValidation.shortages(items, mapOf("missing" to Double.NaN)).single()

        assertEquals(0.0, shortage.available, 0.0)
    }

    private fun item(
        productId: String,
        name: String,
        quantity: Int,
        trackInventory: Boolean = true
    ) = OrderItem(
        productId = productId,
        productName = name,
        quantity = quantity,
        trackInventory = trackInventory
    )
}

package com.laprevia.restobar

import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.data.model.TableStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelDefaultsTest {

    @Test
    fun `product firebase constructor uses safe defaults`() {
        val product = Product()

        assertEquals("", product.id)
        assertEquals("", product.name)
        assertEquals("", product.category)
        assertEquals(null, product.salePrice)
        assertEquals(null, product.costPrice)
        assertFalse(product.trackInventory)
        assertEquals(0.0, product.stock, 0.001)
        assertEquals(0.0, product.minStock, 0.001)
        assertTrue(product.isActive)
        assertEquals(0L, product.createdAt)
        assertEquals(0L, product.updatedAt)
    }

    @Test
    fun `product copy keeps stock and price rules explicit`() {
        val product = Product(
            id = "p1",
            name = "Alitas",
            category = "Comida",
            salePrice = 22.0,
            costPrice = 14.0,
            trackInventory = true,
            stock = 3.0,
            minStock = 5.0,
            isActive = true
        )

        val outOfStock = product.copy(stock = 0.0, isActive = false)

        assertEquals(22.0, outOfStock.salePrice ?: 0.0, 0.001)
        assertEquals(14.0, outOfStock.costPrice ?: 0.0, 0.001)
        assertTrue(outOfStock.trackInventory)
        assertEquals(0.0, outOfStock.stock, 0.001)
        assertEquals(5.0, outOfStock.minStock, 0.001)
        assertFalse(outOfStock.isActive)
    }

    @Test
    fun `table firebase constructor starts free and synced`() {
        val table = Table()

        assertEquals(0, table.id)
        assertEquals(0, table.number)
        assertEquals(TableStatus.LIBRE, table.status)
        assertEquals(null, table.currentOrderId)
        assertEquals(4, table.capacity)
        assertEquals(0L, table.version)
        assertEquals("SYNCED", table.syncStatus)
    }

    @Test
    fun `table copy represents occupied table with active order`() {
        val table = Table(
            id = 4,
            number = 4,
            status = TableStatus.LIBRE
        )

        val occupied = table.copy(
            status = TableStatus.OCUPADA,
            currentOrderId = "order-4",
            syncStatus = "PENDING"
        )

        assertEquals(4, occupied.id)
        assertEquals(TableStatus.OCUPADA, occupied.status)
        assertEquals("order-4", occupied.currentOrderId)
        assertEquals("PENDING", occupied.syncStatus)
    }
}

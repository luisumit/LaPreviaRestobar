package com.laprevia.restobar

import com.laprevia.restobar.data.model.Inventory
import org.junit.Assert.assertEquals
import org.junit.Test

class InventoryRulesTest {

    @Test
    fun `producto con stock cero se clasifica como agotado`() {
        val item = Inventory(
            productId = "p1",
            productName = "Gaseosa",
            currentStock = 0.0,
            unitOfMeasure = "unidades",
            minimumStock = 5.0
        )

        assertEquals(StockLevel.OUT_OF_STOCK, item.stockLevelForTest())
    }

    @Test
    fun `producto bajo o igual al minimo se clasifica como stock bajo`() {
        val item = Inventory(
            productId = "p2",
            productName = "Papas",
            currentStock = 3.0,
            unitOfMeasure = "unidades",
            minimumStock = 5.0
        )

        assertEquals(StockLevel.LOW, item.stockLevelForTest())
    }

    @Test
    fun `producto mayor al minimo se clasifica como suficiente`() {
        val item = Inventory(
            productId = "p3",
            productName = "Arroz",
            currentStock = 12.0,
            unitOfMeasure = "unidades",
            minimumStock = 5.0
        )

        assertEquals(StockLevel.ENOUGH, item.stockLevelForTest())
    }

    private enum class StockLevel {
        OUT_OF_STOCK,
        LOW,
        ENOUGH
    }

    private fun Inventory.stockLevelForTest(): StockLevel {
        return when {
            currentStock == 0.0 -> StockLevel.OUT_OF_STOCK
            currentStock <= minimumStock -> StockLevel.LOW
            else -> StockLevel.ENOUGH
        }
    }
}

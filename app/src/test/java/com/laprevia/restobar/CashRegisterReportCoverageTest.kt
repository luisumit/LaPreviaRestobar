package com.laprevia.restobar

import com.laprevia.restobar.data.local.entity.CashClosureEntity
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.PaymentMethod
import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.service.CashRegisterCalculator
import com.laprevia.restobar.domain.service.SalesCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class CashRegisterReportCoverageTest {

    private val delta = 0.001

    private fun item(productId: String, name: String, quantity: Int, price: Double, subtotal: Double = price * quantity) =
        OrderItem(
            productId = productId,
            productName = name,
            quantity = quantity,
            unitPrice = price,
            subtotal = subtotal
        )

    private fun order(
        id: String,
        status: OrderStatus,
        method: PaymentMethod = PaymentMethod.UNSPECIFIED,
        total: Double = 0.0,
        items: List<OrderItem> = emptyList()
    ) = Order(
        id = id,
        tableNumber = 1,
        status = status,
        paymentMethod = method,
        total = total,
        items = items
    )

    @Test
    fun `reporte de caja suma solo pedidos cobrados y separa metodos de pago`() {
        val completedOrders = listOf(
            order(
                id = "o1",
                status = OrderStatus.COMPLETED,
                method = PaymentMethod.CASH,
                items = listOf(item("p1", "Cerveza", 2, 10.0))
            ),
            order(
                id = "o2",
                status = OrderStatus.COMPLETED,
                method = PaymentMethod.YAPE_PLIN,
                total = 18.0,
                items = listOf(item("p2", "Salchipapa", 1, 18.0))
            ),
            order(
                id = "o3",
                status = OrderStatus.COMPLETED,
                method = PaymentMethod.CARD,
                items = listOf(item("p1", "Cerveza", 1, 10.0))
            )
        )
        val ignoredOrders = listOf(
            order(id = "o4", status = OrderStatus.CANCELLED, method = PaymentMethod.CASH, total = 100.0),
            order(id = "o5", status = OrderStatus.EN_PREPARACION, method = PaymentMethod.CASH, total = 30.0)
        )

        val allOrders = completedOrders + ignoredOrders
        val chargedOrders = allOrders.filter { it.status == OrderStatus.COMPLETED }
        val totalSales = chargedOrders.sumOf { SalesCalculator.orderTotal(it) }
        val cancelledOrders = allOrders.count { it.status == OrderStatus.CANCELLED }

        assertEquals(3, chargedOrders.size)
        assertEquals(48.0, totalSales, delta)
        assertEquals(20.0, SalesCalculator.paymentTotal(chargedOrders, PaymentMethod.CASH), delta)
        assertEquals(18.0, SalesCalculator.paymentTotal(chargedOrders, PaymentMethod.YAPE_PLIN), delta)
        assertEquals(10.0, SalesCalculator.paymentTotal(chargedOrders, PaymentMethod.CARD), delta)
        assertEquals(1, cancelledOrders)
    }

    @Test
    fun `ganancia de caja resta costos y calcula producto mas vendido`() {
        val products = listOf(
            Product(id = "p1", name = "Cerveza", costPrice = 6.0),
            Product(id = "p2", name = "Salchipapa", costPrice = 11.0)
        )
        val chargedOrders = listOf(
            order(
                id = "o1",
                status = OrderStatus.COMPLETED,
                items = listOf(item("p1", "Cerveza", 3, 10.0))
            ),
            order(
                id = "o2",
                status = OrderStatus.COMPLETED,
                items = listOf(item("p2", "Salchipapa", 1, 18.0))
            )
        )

        val topProduct = SalesCalculator.topProducts(chargedOrders, limit = 1).first()

        assertEquals(19.0, SalesCalculator.grossProfit(chargedOrders, products), delta)
        assertEquals(4, SalesCalculator.productsSold(chargedOrders))
        assertEquals("Cerveza", topProduct.name)
        assertEquals(3, topProduct.quantity)
        assertEquals(30.0, topProduct.total, delta)
    }

    @Test
    fun `cierre de caja guarda un resumen fijo del dia`() {
        val closure = CashClosureEntity(
            id = "closure-20260621",
            periodStart = 1_000L,
            periodEnd = 2_000L,
            totalSales = 48.0,
            grossProfit = 19.0,
            chargedOrders = 3,
            cancelledOrders = 1,
            productsSold = 4,
            cashSales = 20.0,
            yapePlinSales = 18.0,
            cardSales = 10.0,
            bestSellingProduct = "Cerveza (3)",
            openingAmount = 50.0,
            incomeAmount = 20.0,
            expenseAmount = 5.0,
            expectedCash = 65.0,
            actualCash = 64.0,
            cashDifference = -1.0,
            createdBy = "Administrador",
            createdAt = 2_001L
        )

        val laterCorrection = closure.copy(totalSales = 60.0)

        assertEquals("closure-20260621", closure.id)
        assertEquals(48.0, closure.totalSales, delta)
        assertEquals(19.0, closure.grossProfit, delta)
        assertEquals(20.0, closure.cashSales, delta)
        assertEquals(18.0, closure.yapePlinSales, delta)
        assertEquals(10.0, closure.cardSales, delta)
        assertEquals("Cerveza (3)", closure.bestSellingProduct)
        assertEquals(50.0, closure.openingAmount, delta)
        assertEquals(20.0, closure.incomeAmount, delta)
        assertEquals(5.0, closure.expenseAmount, delta)
        assertEquals(65.0, closure.expectedCash, delta)
        assertEquals(64.0, closure.actualCash, delta)
        assertEquals(-1.0, closure.cashDifference, delta)
        assertEquals(60.0, laterCorrection.totalSales, delta)
        assertEquals(48.0, closure.totalSales, delta)
    }

    @Test
    fun `arqueo calcula efectivo esperado y diferencia`() {
        val incomeAmount = CashRegisterCalculator.incomeAmount(120.0)
        val expectedCash = CashRegisterCalculator.expectedCash(
            openingAmount = 80.0,
            incomeAmount = incomeAmount,
            expenseAmount = 30.0
        )
        val difference = CashRegisterCalculator.cashDifference(
            actualCash = 172.5,
            expectedCash = expectedCash
        )

        assertEquals(120.0, incomeAmount, delta)
        assertEquals(170.0, expectedCash, delta)
        assertEquals(2.5, difference, delta)
    }

    @Test
    fun `arqueo no permite montos negativos en el esperado`() {
        val incomeAmount = CashRegisterCalculator.incomeAmount(-20.0)
        val expectedCash = CashRegisterCalculator.expectedCash(
            openingAmount = -10.0,
            incomeAmount = incomeAmount,
            expenseAmount = 30.0
        )
        val difference = CashRegisterCalculator.cashDifference(
            actualCash = -5.0,
            expectedCash = expectedCash
        )

        assertEquals(0.0, incomeAmount, delta)
        assertEquals(0.0, expectedCash, delta)
        assertEquals(0.0, difference, delta)
    }

    @Test
    fun `top productos usa nombre alternativo cuando falta nombre`() {
        val chargedOrders = listOf(
            order(
                id = "o1",
                status = OrderStatus.COMPLETED,
                items = listOf(item("p1", "", 2, 5.0))
            )
        )

        val topProduct = SalesCalculator.topProducts(chargedOrders).first()

        assertEquals("Producto sin nombre", topProduct.name)
        assertEquals(2, topProduct.quantity)
        assertEquals(10.0, topProduct.total, delta)
    }
}

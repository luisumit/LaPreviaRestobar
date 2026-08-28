package com.laprevia.restobar

import com.laprevia.restobar.domain.model.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del Value Object Money: operaciones, porcentaje, no-negatividad,
 * comparacion, igualdad por valor y formato.
 */
class MoneyTest {

    private val delta = 0.001

    @Test
    fun `suma de dinero`() {
        assertEquals(30.0, (Money(20.0) + Money(10.0)).amount, delta)
    }

    @Test
    fun `resta de dinero`() {
        assertEquals(15.0, (Money(20.0) - Money(5.0)).amount, delta)
    }

    @Test
    fun `multiplicacion por cantidad`() {
        assertEquals(30.0, (Money(10.0) * 3).amount, delta)
    }

    @Test
    fun `porcentaje de un monto`() {
        assertEquals(10.0, Money(50.0).percentage(20).amount, delta)
    }

    @Test
    fun `porcentaje cero no descuenta`() {
        assertEquals(0.0, Money(50.0).percentage(0).amount, delta)
    }

    @Test
    fun `atLeastZero evita negativos`() {
        assertEquals(0.0, (Money(10.0) - Money(30.0)).atLeastZero().amount, delta)
        assertEquals(5.0, Money(5.0).atLeastZero().amount, delta)
    }

    @Test
    fun `isPositive distingue montos`() {
        assertTrue(Money(1.0).isPositive())
        assertFalse(Money(0.0).isPositive())
    }

    @Test
    fun `se comparan por monto`() {
        assertTrue(Money(20.0) > Money(10.0))
        assertTrue(Money(5.0) < Money(10.0))
    }

    @Test
    fun `igualdad por valor (value object)`() {
        assertEquals(Money(12.5), Money(12.5))
    }

    @Test
    fun `formato en soles`() {
        assertEquals("S/ 12.50", Money(12.5).formatted())
    }
}

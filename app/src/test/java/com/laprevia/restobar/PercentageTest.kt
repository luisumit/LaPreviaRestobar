package com.laprevia.restobar

import com.laprevia.restobar.domain.model.Money
import com.laprevia.restobar.domain.model.Percentage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del Value Object Percentage y su colaboracion con Money.
 */
class PercentageTest {

    private val delta = 0.001

    @Test
    fun `porcentaje aplicado a un monto`() {
        assertEquals(10.0, Percentage(20).of(Money(50.0)).amount, delta)
    }

    @Test
    fun `porcentaje cero es cero`() {
        assertTrue(Percentage(0).isZero)
        assertEquals(0.0, Percentage(0).of(Money(50.0)).amount, delta)
    }

    @Test
    fun `negativo se acota a cero`() {
        assertEquals(0, Percentage.of(-10).value)
    }

    @Test
    fun `mayor a cien se acota a cien`() {
        assertEquals(100, Percentage.of(200).value)
    }

    @Test
    fun `isZero distingue`() {
        assertFalse(Percentage(15).isZero)
        assertTrue(Percentage.NONE.isZero)
    }

    @Test
    fun `se comparan por valor`() {
        assertTrue(Percentage(20) > Percentage(10))
    }

    @Test
    fun `formato con simbolo`() {
        assertEquals("20%", Percentage(20).formatted())
    }
}

package com.laprevia.restobar

import com.laprevia.restobar.domain.SplitBill
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas de division de cuenta: partes iguales y por comensal.
 */
class SplitBillTest {

    private val delta = 0.001

    // ---------- Partes iguales ----------

    @Test
    fun `divide en partes iguales`() {
        assertEquals(25.0, SplitBill.perPerson(100.0, 4), delta)
    }

    @Test
    fun `division con decimales`() {
        assertEquals(33.333, SplitBill.perPerson(100.0, 3), delta)
    }

    @Test
    fun `con cero personas devuelve el total (no divide por cero)`() {
        assertEquals(100.0, SplitBill.perPerson(100.0, 0), delta)
    }

    // ---------- Por comensal ----------

    @Test
    fun `asigna cada item a su comensal`() {
        // item0 -> comensal 1, item1 -> comensal 2, item2 -> comensal 2
        val subtotals = listOf(30.0, 12.5, 24.0)
        val assignment = mapOf(0 to 1, 1 to 2, 2 to 2)
        val result = SplitBill.perComensal(subtotals, assignment, people = 2)

        assertEquals(30.0, result[1]!!, delta)
        assertEquals(36.5, result[2]!!, delta)
    }

    @Test
    fun `item sin asignar cuenta para el comensal 1`() {
        val subtotals = listOf(20.0, 15.0)
        val assignment = mapOf(0 to 2) // item1 sin asignar
        val result = SplitBill.perComensal(subtotals, assignment, people = 2)

        assertEquals(15.0, result[1]!!, delta) // el no asignado
        assertEquals(20.0, result[2]!!, delta)
    }

    @Test
    fun `la suma por comensal es igual al total`() {
        val subtotals = listOf(30.0, 12.5, 24.0)
        val assignment = mapOf(0 to 1, 1 to 3, 2 to 2)
        val result = SplitBill.perComensal(subtotals, assignment, people = 3)

        assertEquals(subtotals.sum(), result.values.sum(), delta)
    }
}

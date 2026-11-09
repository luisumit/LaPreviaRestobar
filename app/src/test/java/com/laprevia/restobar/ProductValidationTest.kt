package com.laprevia.restobar

import com.laprevia.restobar.domain.ProductValidation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductValidationTest {
    @Test
    fun `parsea decimales con punto o coma`() {
        assertEquals(12.5, ProductValidation.parseDecimal("12.5")!!, 0.0)
        assertEquals(12.5, ProductValidation.parseDecimal("12,5")!!, 0.0)
        assertEquals(0.0, ProductValidation.parseDecimal("0")!!, 0.0)
    }

    @Test
    fun `rechaza negativos y entradas no numericas`() {
        assertNull(ProductValidation.parseDecimal("-1"))
        assertNull(ProductValidation.parseDecimal("abc"))
        assertNull(ProductValidation.parseDecimal("NaN"))
        assertNull(ProductValidation.parseDecimal("Infinity"))
        assertNull(ProductValidation.parseDecimal(""))
    }

    @Test
    fun `filtra letras y conserva un separador decimal`() {
        assertEquals("12.34", ProductValidation.filterNumericInput("a12,34x"))
        assertEquals("7.5", ProductValidation.filterNumericInput("7,,5x"))
        assertTrue(ProductValidation.filterNumericInput("abc").isEmpty())
    }
}

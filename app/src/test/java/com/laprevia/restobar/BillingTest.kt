package com.laprevia.restobar

import com.laprevia.restobar.domain.Billing
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pruebas de la logica de plata: descuentos, total neto y vuelto.
 * Es lo mas critico del cobro, por eso se cubre con casos limite.
 */
class BillingTest {

    private val delta = 0.001

    // ---------- Descuento por porcentaje ----------

    @Test
    fun `descuento de 20 porciento sobre 50 es 10`() {
        assertEquals(10.0, Billing.discountFromPercent(50.0, 20), delta)
    }

    @Test
    fun `porcentaje cero no genera descuento`() {
        assertEquals(0.0, Billing.discountFromPercent(50.0, 0), delta)
    }

    @Test
    fun `porcentaje negativo no genera descuento`() {
        assertEquals(0.0, Billing.discountFromPercent(50.0, -10), delta)
    }

    @Test
    fun `descuento sobre total cero es cero`() {
        assertEquals(0.0, Billing.discountFromPercent(0.0, 20), delta)
    }

    // ---------- Total neto ----------

    @Test
    fun `sin descuento el neto es igual al total`() {
        assertEquals(50.0, Billing.netTotal(50.0, 0.0), delta)
    }

    @Test
    fun `neto con descuento normal`() {
        assertEquals(40.0, Billing.netTotal(50.0, 10.0), delta)
    }

    @Test
    fun `descuento mayor al total deja el neto en cero`() {
        assertEquals(0.0, Billing.netTotal(50.0, 80.0), delta)
    }

    @Test
    fun `descuento negativo se ignora`() {
        assertEquals(50.0, Billing.netTotal(50.0, -5.0), delta)
    }

    // ---------- Vuelto ----------

    @Test
    fun `vuelto normal`() {
        assertEquals(50.0, Billing.change(100.0, 50.0), delta)
    }

    @Test
    fun `vuelto exacto es cero`() {
        assertEquals(0.0, Billing.change(50.0, 50.0), delta)
    }

    @Test
    fun `vuelto nunca es negativo si recibe de menos`() {
        assertEquals(0.0, Billing.change(30.0, 50.0), delta)
    }

    // ---------- Flujo completo (Happy Hour) ----------

    @Test
    fun `flujo completo con 20 porciento y pago en efectivo`() {
        val total = 66.5
        val discount = Billing.discountFromPercent(total, 20) // 13.30
        val net = Billing.netTotal(total, discount)           // 53.20
        val change = Billing.change(100.0, net)               // 46.80

        assertEquals(13.30, discount, delta)
        assertEquals(53.20, net, delta)
        assertEquals(46.80, change, delta)
    }
}

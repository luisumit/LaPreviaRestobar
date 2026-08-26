package com.laprevia.restobar.domain

import com.laprevia.restobar.domain.model.Money
import com.laprevia.restobar.domain.model.Percentage
import kotlin.math.max

/**
 * Calculos de cobro (descuentos, neto y vuelto). Es codigo PURO — sin Android ni
 * dependencias — para que sea facil de probar y reutilizar en el ViewModel y la UI.
 */
object Billing {

    /** Monto de descuento a partir de un porcentaje (0..100) sobre el total. */
    fun discountFromPercent(total: Double, percent: Int): Double =
        Percentage.of(percent).of(Money(total)).amount

    /** Total neto tras aplicar el descuento (nunca negativo; el descuento se acota al total). */
    fun netTotal(total: Double, discount: Double): Double {
        val safeDiscount = discount.coerceIn(0.0, max(0.0, total))
        return (total - safeDiscount).coerceAtLeast(0.0)
    }

    /** Vuelto = recibido - neto (nunca negativo). */
    fun change(received: Double, netTotal: Double): Double {
        return (received - netTotal).coerceAtLeast(0.0)
    }
}

package com.laprevia.restobar.domain.model

import java.util.Locale

/**
 * Value Object (DDD): representa un monto de dinero en soles.
 *
 * Es **inmutable** y se compara **por valor** (dos Money con el mismo monto son iguales).
 * Encapsula las operaciones de dinero para que el dominio no ande sumando `Double` sueltos.
 * No se persiste directamente: en los bordes (Room/Firebase) se convierte a/desde Double.
 */
data class Money(val amount: Double) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(amount + other.amount)

    operator fun minus(other: Money): Money = Money(amount - other.amount)

    operator fun times(quantity: Int): Money = Money(amount * quantity)

    /** Monto que corresponde a un porcentaje de este dinero (ej. descuento 20%). */
    fun percentage(percent: Int): Money =
        if (percent <= 0 || amount <= 0.0) ZERO else Money(amount * percent / 100.0)

    /** Nunca por debajo de cero — evita totales o vueltos negativos. */
    fun atLeastZero(): Money = if (amount < 0.0) ZERO else this

    fun isPositive(): Boolean = amount > 0.0

    override fun compareTo(other: Money): Int = amount.compareTo(other.amount)

    /** Formato para mostrar/imprimir: "S/ 12.50". */
    fun formatted(): String = "S/ " + String.format(Locale.US, "%.2f", amount)

    companion object {
        val ZERO = Money(0.0)
        fun of(amount: Double): Money = Money(amount)
    }
}

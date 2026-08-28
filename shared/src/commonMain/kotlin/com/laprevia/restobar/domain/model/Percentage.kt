package com.laprevia.restobar.domain.model

/**
 * Value Object (DDD): un porcentaje válido (0..100), típicamente un descuento.
 *
 * Inmutable y comparado por valor. Colabora con [Money]: `Percentage(20).of(precio)`
 * devuelve el monto correspondiente. Se acota a 0..100 para que sea siempre válido.
 */
data class Percentage(val value: Int) : Comparable<Percentage> {

    val isZero: Boolean get() = value <= 0

    /** Monto que representa este porcentaje sobre un dinero (ej. 20% de S/50 = S/10). */
    fun of(money: Money): Money = money.percentage(value)

    override fun compareTo(other: Percentage): Int = value.compareTo(other.value)

    fun formatted(): String = "$value%"

    companion object {
        val NONE = Percentage(0)

        /** Crea un porcentaje acotado a 0..100. */
        fun of(value: Int): Percentage = Percentage(value.coerceIn(0, 100))
    }
}

package com.laprevia.restobar.domain

/**
 * Calculo de division de cuenta: en partes iguales o asignando cada producto a un
 * comensal. Puro y testeable — no depende de Android ni de la UI.
 */
object SplitBill {

    /** Monto que paga cada persona al dividir en partes iguales. */
    fun perPerson(total: Double, people: Int): Double {
        if (people <= 0) return total
        return total / people
    }

    /**
     * Total por comensal segun la asignacion de cada item.
     * @param subtotals subtotal de cada item, por indice.
     * @param assignment mapa indiceDelItem -> numeroDeComensal (1..people). Si un item
     *                   no esta asignado, cuenta para el comensal 1.
     * @return mapa numeroDeComensal (1..people) -> total a pagar.
     */
    fun perComensal(subtotals: List<Double>, assignment: Map<Int, Int>, people: Int): Map<Int, Double> {
        val result = (1..people).associateWith { 0.0 }.toMutableMap()
        subtotals.forEachIndexed { index, subtotal ->
            val comensal = (assignment[index] ?: 1).coerceIn(1, maxOf(1, people))
            result[comensal] = (result[comensal] ?: 0.0) + subtotal
        }
        return result
    }
}

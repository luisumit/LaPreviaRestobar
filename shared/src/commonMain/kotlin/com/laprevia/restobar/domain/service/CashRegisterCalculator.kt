package com.laprevia.restobar.domain.service

object CashRegisterCalculator {

    fun incomeAmount(cashSales: Double): Double =
        cashSales.coerceAtLeast(0.0)

    fun expectedCash(openingAmount: Double, incomeAmount: Double, expenseAmount: Double): Double =
        (openingAmount.coerceAtLeast(0.0) +
            incomeAmount.coerceAtLeast(0.0) -
            expenseAmount.coerceAtLeast(0.0)).coerceAtLeast(0.0)

    fun cashDifference(actualCash: Double, expectedCash: Double): Double =
        actualCash.coerceAtLeast(0.0) - expectedCash
}

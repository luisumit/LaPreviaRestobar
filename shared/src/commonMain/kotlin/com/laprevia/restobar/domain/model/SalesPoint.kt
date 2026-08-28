package com.laprevia.restobar.domain.model

/** Punto de la serie de ventas por periodo/hora (para gráficos de tendencia y horas pico). */
data class DailySalesPoint(
    val label: String,
    val total: Double
)

/** Producto vendido con su cantidad y total (para el ranking de top productos). */
data class ProductSalesPoint(
    val name: String,
    val quantity: Int,
    val total: Double
)

package com.laprevia.restobar.data.model

data class Inventory(
    val productId: String,
    val productName: String,
    val currentStock: Double,
    val unitOfMeasure: String,
    val minimumStock: Double = 0.0,
    val category: String? = null
)
package com.laprevia.restobar.data.model
data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "",
    val salePrice: Double? = null,
    val costPrice: Double? = null,
    val trackInventory: Boolean = false,
    val stock: Double = 0.0,
    val minStock: Double = 0.0,
    val imageUrl: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Constructor sin argumentos para Firebase
    constructor() : this(
        id = "",
        name = "",
        description = "",
        category = "",
        salePrice = null,
        costPrice = null,
        trackInventory = false,
        stock = 0.0,
        minStock = 0.0,
        imageUrl = null,
        isActive = true,
        createdAt = 0,
        updatedAt = 0
    )
}
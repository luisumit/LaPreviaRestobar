// data/remote/dto/ProductDto.kt - VERSIÓN CORREGIDA
package com.laprevia.restobar.data.remote.dto

import com.laprevia.restobar.data.model.Product

// DTO principal para productos
data class ProductDto(
    val id: String,
    val name: String,
    val description: String = "",
    val category: String = "General",
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
    fun toProduct(): Product {
        return Product(
            id = id,
            name = name,
            description = description,
            category = category,
            salePrice = salePrice,
            costPrice = costPrice,
            trackInventory = trackInventory,
            stock = stock,
            minStock = minStock,
            imageUrl = imageUrl,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

// Extension function para convertir Product a ProductDto
fun Product.toProductDto(): ProductDto {
    return ProductDto(
        id = id,
        name = name,
        description = description ?: "",
        category = category,
        salePrice = salePrice,
        costPrice = costPrice,
        trackInventory = trackInventory,
        stock = stock,
        minStock = minStock,
        imageUrl = imageUrl,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

package com.laprevia.restobar.data.mapper

import com.laprevia.restobar.data.local.entity.ProductEntity
import com.laprevia.restobar.data.model.Product

fun ProductEntity.toDomain(): Product {
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
        imageUrl = null,  // Puedes agregar si lo necesitas
        isActive = isActive,
        createdAt = lastModified,  // Usar lastModified como createdAt
        updatedAt = updatedAt,     // ✅ AGREGAR updatedAt
        version = version          // ✅ AGREGAR version
    )
}

fun Product.toEntity(): ProductEntity {
    return ProductEntity(
        id = id,
        name = name,
        description = description,
        category = category,
        salePrice = salePrice,
        costPrice = costPrice,
        trackInventory = trackInventory,
        stock = stock,
        minStock = minStock,
        isActive = isActive,
        syncStatus = "PENDING",
        version = System.currentTimeMillis(),      // ✅ AGREGAR version
        lastModified = System.currentTimeMillis(), // ✅ AGREGAR lastModified
        updatedAt = updatedAt                      // ✅ AGREGAR updatedAt
    )
}
package com.laprevia.restobar.data.mapper

import com.laprevia.restobar.data.local.entity.InventoryEntity
import com.laprevia.restobar.data.model.Inventory

fun InventoryEntity.toDomain(): Inventory {
    return Inventory(
        productId = productId,
        productName = productName,
        currentStock = currentStock,
        unitOfMeasure = unitOfMeasure,
        minimumStock = minimumStock,
        category = category
    )
}

fun Inventory.toEntity(): InventoryEntity {
    return InventoryEntity(
        productId = productId,
        productName = productName,
        currentStock = currentStock,
        unitOfMeasure = unitOfMeasure,
        minimumStock = minimumStock,
        category = category,
        syncStatus = "PENDING"
    )
}

// data/remote/dto/InventoryDto.kt - ARCHIVO NUEVO
package com.laprevia.restobar.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.laprevia.restobar.data.model.Inventory

data class InventoryDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("productId")
    val productId: Long,

    @SerializedName("productName")
    val productName: String,

    @SerializedName("stockQuantity")
    val stockQuantity: Int,

    @SerializedName("category")
    val category: String,

    @SerializedName("minimumStock")
    val minimumStock: Int = 10
) {
    fun toInventory(): Inventory {
        return Inventory(
            productId = productId.toString(),
            productName = productName,
            currentStock = stockQuantity.toDouble(),
            unitOfMeasure = "unidades",
            minimumStock = minimumStock.toDouble(),
            category = category
        )
    }
}
// data/remote/dto/RequestDTOs.kt - COMPLETO
package com.laprevia.restobar.data.remote.dto

import com.google.gson.annotations.SerializedName

// DTOs para requests
data class StatusUpdateRequest(
    @SerializedName("status")
    val status: String
)

data class OrderStatusUpdateRequest(
    @SerializedName("status")
    val status: String
)

data class StockUpdateRequest(
    @SerializedName("stock")
    val stock: Int
)

data class CreateProductDto(
    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("price")
    val price: Double,

    @SerializedName("category")
    val category: String,

    @SerializedName("stock")
    val stock: Int
)

data class UpdateProductDto(
    @SerializedName("name")
    val name: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("price")
    val price: Double,

    @SerializedName("category")
    val category: String,

    @SerializedName("stock")
    val stock: Int
)

data class CreateOrderDto(
    @SerializedName("tableId")
    val tableId: Int,

    @SerializedName("items")
    val items: List<OrderItemDto>,

    @SerializedName("total")
    val total: Double
) {
    data class OrderItemDto(
        @SerializedName("productId")
        val productId: String,

        @SerializedName("quantity")
        val quantity: Int,

        @SerializedName("unitPrice")
        val unitPrice: Double,

        @SerializedName("notes")
        val notes: String? = null
    )
}

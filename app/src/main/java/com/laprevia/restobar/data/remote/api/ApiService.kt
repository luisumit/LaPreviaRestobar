// data/remote/api/ApiService.kt - SOLO INTERFAZ
package com.laprevia.restobar.data.remote.api

import com.laprevia.restobar.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService{

    // ✅ PRODUCT ENDPOINTS
    @GET("products")
    suspend fun getProducts(): List<ProductDto>

    @GET("products/{id}")
    suspend fun getProductById(@Path("id") id: String): ProductDto

    @POST("products")
    suspend fun createProduct(@Body product: CreateProductDto): ProductDto

    @PUT("products/{id}")
    suspend fun updateProduct(@Path("id") id: String, @Body product: UpdateProductDto): ProductDto

    @DELETE("products/{id}")
    suspend fun deleteProduct(@Path("id") id: String): Response<Void>

    @GET("products/categories")
    suspend fun getCategories(): List<String>

    // ✅ TABLE ENDPOINTS
    @GET("tables")
    suspend fun getTables(): List<TableDto>

    @GET("tables/{id}")
    suspend fun getTableById(@Path("id") id: Int): TableDto

    @PUT("tables/{id}/status")
    suspend fun updateTableStatus(
        @Path("id") id: Int,
        @Body statusUpdate: StatusUpdateRequest
    ): TableDto

    // ✅ ORDER ENDPOINTS
    @GET("orders")
    suspend fun getOrders(): List<OrderDto>

    @GET("orders/table/{tableId}")
    suspend fun getOrdersByTable(@Path("tableId") tableId: Int): List<OrderDto>

    @POST("orders")
    suspend fun createOrder(@Body order: CreateOrderDto): OrderDto

    @PUT("orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") id: String,
        @Body statusUpdate: OrderStatusUpdateRequest
    ): OrderDto

    // ✅ INVENTORY ENDPOINTS
    @GET("inventory")
    suspend fun getInventory(): List<InventoryDto>

    @PUT("inventory/{productId}/stock")
    suspend fun updateInventoryStock(
        @Path("productId") productId: String,
        @Body stockUpdate: StockUpdateRequest
    ): InventoryDto
    @GET("api/health")  // 🔥 Esto se convierte en: http://192.168.0.104:8080/api/health
    suspend fun healthCheck(): Response<HealthResponse>

    // Otras rutas de tu API...

    data class HealthResponse(
        val status: String,
        val message: String,
        val timestamp: Long)
}
// ❌ NO DATA CLASSES AQUÍ - todas están en RequestDTOs.kt

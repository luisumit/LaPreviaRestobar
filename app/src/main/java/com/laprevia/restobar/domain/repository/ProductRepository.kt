package com.laprevia.restobar.domain.repository

import com.laprevia.restobar.data.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getAllProducts(): Flow<List<Product>>
    fun getActiveProducts(): Flow<List<Product>>
    fun getSellableProducts(): Flow<List<Product>>

    // ✅ CAMBIA ESTE MÉTODO - de Product? a Product
    suspend fun getProductById(id: String): Product  // ❌ ANTES: Product? | ✅ AHORA: Product

    suspend fun getProductByName(name: String): Product?
    suspend fun createProduct(product: Product)
    suspend fun updateProduct(product: Product)
    suspend fun updateProductStatus(id: String, isActive: Boolean)
    suspend fun deleteProduct(id: String)
    fun getCategories(): Flow<List<String>>
    suspend fun addProduct(product: Product)
    suspend fun removeProduct(productId: String)

    // Método para compatibilidad (opcional)
    fun getProducts(): Flow<List<Product>> = getAllProducts()

    // MÉTODOS PARA INVENTARIO:
    fun getProductsWithInventory(): Flow<List<Product>>
    suspend fun updateProductStock(productId: String, newQuantity: Double)
}
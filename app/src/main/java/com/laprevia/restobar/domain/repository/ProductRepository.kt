package com.laprevia.restobar.domain.repository

import com.laprevia.restobar.data.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {

    // ==================== LISTADOS ====================
    fun getAllProducts(): Flow<List<Product>>
    fun getActiveProducts(): Flow<List<Product>>
    fun getSellableProducts(): Flow<List<Product>>

    // ==================== BÚSQUEDA ====================
    suspend fun getProductById(id: String): Product
    suspend fun getProductByName(name: String): Product?

    // ==================== CRUD ====================
    suspend fun createProduct(product: Product)
    suspend fun updateProduct(product: Product)
    suspend fun deleteProduct(id: String)

    // ==================== ESTADO ====================
    suspend fun updateProductStatus(id: String, isActive: Boolean)

    // ==================== CATEGORÍAS ====================
    fun getCategories(): Flow<List<String>>

    // ==================== COMPATIBILIDAD (OPCIONAL) ====================
    fun getProducts(): Flow<List<Product>> = getAllProducts()

    // ==================== INVENTARIO ====================
    fun getProductsWithInventory(): Flow<List<Product>>

    // ✅ CORREGIDO: Cambiar newQuantity a newStock para consistencia
    suspend fun updateProductStock(productId: String, newStock: Double)

    // ✅ NUEVO MÉTODO: Obtener stock de un producto
    suspend fun getProductStock(productId: String): Double
}
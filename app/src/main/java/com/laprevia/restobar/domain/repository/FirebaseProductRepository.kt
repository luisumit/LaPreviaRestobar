package com.laprevia.restobar.domain.repository

import com.laprevia.restobar.data.model.Product
import kotlinx.coroutines.flow.Flow

interface FirebaseProductRepository : ProductRepository {
    // Métodos específicos para Firebase (tiempo real)
    fun listenToProductChanges(): Flow<Product>
    fun getProductsRealTime(): Flow<List<Product>>


    // ✅ MÉTODOS ADICIONALES ÚTILES
    suspend fun searchProducts(query: String): List<Product>
    suspend fun getProductsByCategory(category: String): List<Product>
    suspend fun productExists(productId: String): Boolean
    suspend fun getProductStats(): Map<String, Any>

}
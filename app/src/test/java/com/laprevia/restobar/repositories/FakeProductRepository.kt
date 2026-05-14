package com.laprevia.restobar.repositories

import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.repository.FirebaseProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeProductRepository : FirebaseProductRepository {
    private val products = mutableListOf<Product>()

    // ==================== FirebaseProductRepository ====================
    override fun listenToProductChanges(): Flow<Product> = flowOf()
    override fun getProductsRealTime(): Flow<List<Product>> = flowOf(products)

    // ==================== ProductRepository ====================
    override fun getAllProducts(): Flow<List<Product>> = flowOf(products)
    override fun getActiveProducts(): Flow<List<Product>> = flowOf(products.filter { it.isActive })
    override fun getSellableProducts(): Flow<List<Product>> = flowOf(products.filter { it.isActive })
    override fun getCategories(): Flow<List<String>> = flowOf(products.mapNotNull { it.category }.distinct())
    override fun getProducts(): Flow<List<Product>> = flowOf(products)
    override fun getProductsWithInventory(): Flow<List<Product>> = flowOf(products.filter { it.trackInventory })

    override suspend fun getProductById(id: String): Product = products.find { it.id == id } ?: throw Exception("Product not found")
    override suspend fun getProductByName(name: String): Product? = products.find { it.name == name }
    override suspend fun createProduct(product: Product) { products.add(product) }
    override suspend fun updateProduct(product: Product) {
        val index = products.indexOfFirst { it.id == product.id }
        if (index != -1) products[index] = product
    }
    override suspend fun deleteProduct(id: String) { products.removeAll { it.id == id } }
    override suspend fun updateProductStatus(id: String, isActive: Boolean) {
        val index = products.indexOfFirst { it.id == id }
        if (index != -1) products[index] = products[index].copy(isActive = isActive)
    }

    override suspend fun getProductStock(productId: String): Double = products.find { it.id == productId }?.stock ?: 0.0
    override suspend fun updateProductStock(productId: String, newStock: Double) {
        val index = products.indexOfFirst { it.id == productId }
        if (index != -1) products[index] = products[index].copy(stock = newStock)
    }

    override suspend fun searchProducts(query: String): List<Product> = products.filter { it.name.contains(query, ignoreCase = true) }
    override suspend fun getProductsByCategory(category: String): List<Product> = products.filter { it.category == category }
    override suspend fun productExists(productId: String): Boolean = products.any { it.id == productId }
    override suspend fun getProductStats(): Map<String, Any> = mapOf(
        "totalProducts" to products.size,
        "activeProducts" to products.count { it.isActive },
        "trackInventoryProducts" to products.count { it.trackInventory },
        "categoriesCount" to products.map { it.category }.distinct().size
    )
}
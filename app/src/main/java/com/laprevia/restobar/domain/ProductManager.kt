package com.laprevia.restobar.domain

import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProductManager @Inject constructor(
    private val productRepository: ProductRepository
) {
    val products: Flow<List<Product>>
        get() = productRepository.getProducts()

    val sellableProducts: Flow<List<Product>>
        get() = productRepository.getSellableProducts()

    // ✅ CORREGIDO: usar createProduct en lugar de addProduct
    suspend fun addProduct(product: Product) {
        productRepository.createProduct(product)
    }

    suspend fun updateProduct(product: Product) {
        productRepository.updateProduct(product)
    }

    // ✅ CORREGIDO: usar deleteProduct en lugar de removeProduct
    suspend fun removeProduct(productId: String) {
        productRepository.deleteProduct(productId)
    }
}

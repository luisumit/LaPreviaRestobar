
package com.laprevia.restobar.domain.usecase

import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

class GetProductsUseCase constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(): Flow<List<Product>> {
        return productRepository.getProducts()
    }
}

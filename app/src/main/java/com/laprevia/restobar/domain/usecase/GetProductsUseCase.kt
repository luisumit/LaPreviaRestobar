
package com.laprevia.restobar.domain.usecase

import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProductsUseCase @Inject constructor(
    private val productRepository: ProductRepository
) {
    operator fun invoke(): Flow<List<Product>> {
        return productRepository.getProducts()
    }
}

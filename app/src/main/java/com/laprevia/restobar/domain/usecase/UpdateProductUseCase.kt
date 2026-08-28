// domain/usecase/UpdateProductUseCase.kt
package com.laprevia.restobar.domain.usecase

import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.repository.ProductRepository

class UpdateProductUseCase constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(product: Product) {
        productRepository.updateProduct(product)
    }
}

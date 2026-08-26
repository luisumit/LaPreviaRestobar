// domain/usecase/DeleteProductUseCase.kt
package com.laprevia.restobar.domain.usecase

import com.laprevia.restobar.domain.repository.ProductRepository

class DeleteProductUseCase constructor(
    private val productRepository: ProductRepository
) {
    suspend operator fun invoke(productId: String) {
        productRepository.deleteProduct(productId)
    }
}

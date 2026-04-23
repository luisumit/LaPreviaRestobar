// di/UseCaseModule.kt
package com.laprevia.restobar.di

import com.laprevia.restobar.domain.usecase.*
import com.laprevia.restobar.domain.repository.FirebaseOrderRepository
import com.laprevia.restobar.domain.repository.FirebaseProductRepository
import com.laprevia.restobar.domain.repository.FirebaseTableRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    // ✅ Order Use Cases
    @Provides
    @Singleton
    fun provideCreateOrderUseCase(
        firebaseOrderRepository: FirebaseOrderRepository,
        firebaseTableRepository: FirebaseTableRepository
    ): CreateOrderUseCase {
        return CreateOrderUseCase(firebaseOrderRepository, firebaseTableRepository)
    }

    @Provides
    @Singleton
    fun provideUpdateOrderStatusUseCase(
        firebaseOrderRepository: FirebaseOrderRepository
    ): UpdateOrderStatusUseCase {
        return UpdateOrderStatusUseCase(firebaseOrderRepository)
    }

    // ✅ Product Use Cases
    @Provides
    @Singleton
    fun provideCreateProductUseCase(
        firebaseProductRepository: FirebaseProductRepository
    ): CreateProductUseCase {
        return CreateProductUseCase(firebaseProductRepository)
    }

    @Provides
    @Singleton
    fun provideGetProductsUseCase(
        firebaseProductRepository: FirebaseProductRepository
    ): GetProductsUseCase {
        return GetProductsUseCase(firebaseProductRepository)
    }

    @Provides
    @Singleton
    fun provideUpdateProductUseCase(
        firebaseProductRepository: FirebaseProductRepository
    ): UpdateProductUseCase {
        return UpdateProductUseCase(firebaseProductRepository)
    }

    @Provides
    @Singleton
    fun provideDeleteProductUseCase(
        firebaseProductRepository: FirebaseProductRepository
    ): DeleteProductUseCase {
        return DeleteProductUseCase(firebaseProductRepository)
    }

    // ✅ Table Use Cases
    @Provides
    @Singleton
    fun provideGetTablesUseCase(
        firebaseTableRepository: FirebaseTableRepository
    ): GetTablesUseCase {
        return GetTablesUseCase(firebaseTableRepository)
    }
}
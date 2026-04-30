package com.laprevia.restobar.di

import com.laprevia.restobar.data.repository.*
import com.laprevia.restobar.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // ================= ORDERS =================
    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        impl: FirebaseOrderRepositoryImpl
    ): FirebaseOrderRepository

    // ================= TABLES =================
    @Binds
    @Singleton
    abstract fun bindTableRepository(
        impl: FirebaseTableRepositoryImpl
    ): FirebaseTableRepository

    // ================= PRODUCTS =================
    @Binds
    @Singleton
    abstract fun bindProductRepository(
        impl: FirebaseProductRepositoryImpl
    ): FirebaseProductRepository

    // ================= INVENTORY =================
    @Binds
    @Singleton
    abstract fun bindInventoryRepository(
        impl: FirebaseInventoryRepositoryImpl
    ): FirebaseInventoryRepository
}
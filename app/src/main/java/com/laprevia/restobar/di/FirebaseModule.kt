package com.laprevia.restobar.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.laprevia.restobar.data.repository.*
import com.laprevia.restobar.domain.repository.*
import com.laprevia.restobar.domain.service.FirebaseInitializerService
import com.laprevia.restobar.domain.service.InventorySyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    // ✅ AGREGAR: Firebase Auth
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return Firebase.auth
    }

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance().apply {
            setPersistenceEnabled(true)
        }
    }

    @Provides
    @Singleton
    @OrdersReference
    fun provideOrdersReference(database: FirebaseDatabase): DatabaseReference {
        return database.getReference("orders")
    }

    @Provides
    @Singleton
    @TablesReference
    fun provideTablesReference(database: FirebaseDatabase): DatabaseReference {
        return database.getReference("tables")
    }

    @Provides
    @Singleton
    @ProductsReference
    fun provideProductsReference(database: FirebaseDatabase): DatabaseReference {
        return database.getReference("products")
    }

    @Provides
    @Singleton
    @InventoryReference
    fun provideInventoryReference(database: FirebaseDatabase): DatabaseReference {
        return database.getReference("inventory")
    }

    @Provides
    @Singleton
    fun provideFirebaseOrderRepository(
        @OrdersReference ordersRef: DatabaseReference
    ): FirebaseOrderRepository {
        return FirebaseOrderRepositoryImpl(ordersRef)
    }

    @Provides
    @Singleton
    fun provideFirebaseTableRepository(
        @TablesReference tablesRef: DatabaseReference
    ): FirebaseTableRepository {
        return FirebaseTableRepositoryImpl(tablesRef)
    }

    @Provides
    @Singleton
    fun provideFirebaseProductRepository(
        @ProductsReference productsRef: DatabaseReference
    ): FirebaseProductRepository {
        return FirebaseProductRepositoryImpl(productsRef)
    }

    @Provides
    @Singleton
    fun provideFirebaseInventoryRepository(
        @InventoryReference inventoryRef: DatabaseReference
    ): FirebaseInventoryRepository {
        return FirebaseInventoryRepositoryImpl(inventoryRef)
    }

    @Provides
    @Singleton
    fun provideFirebaseInitializerService(
        firebaseTableRepository: FirebaseTableRepository,
        firebaseProductRepository: FirebaseProductRepository,
        firebaseOrderRepository: FirebaseOrderRepository,
        firebaseInventoryRepository: FirebaseInventoryRepository
    ): FirebaseInitializerService {
        return FirebaseInitializerService(
            firebaseTableRepository,
            firebaseProductRepository,
            firebaseOrderRepository,
            firebaseInventoryRepository
        )
    }

    @Provides
    @Singleton
    fun provideInventorySyncService(
        productRepository: FirebaseProductRepository,
        inventoryRepository: FirebaseInventoryRepository
    ): InventorySyncService {
        return InventorySyncService(productRepository, inventoryRepository)
    }
}
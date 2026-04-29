package com.laprevia.restobar.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
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

    // ================= FIREBASE AUTH =================
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return Firebase.auth
    }

    // ================= FIREBASE DATABASE =================
    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase {
        return FirebaseDatabase.getInstance().apply {
            try {
                setPersistenceEnabled(true)
            } catch (e: Exception) {
                // ya estaba configurado
            }
        }
    }

    // ================= REFERENCES =================

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

    // ================= SERVICES =================

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

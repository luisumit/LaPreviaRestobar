// di/AppModule.kt
package com.laprevia.restobar.di

import android.content.Context
import com.laprevia.restobar.data.local.datastore.PreferencesManager
import com.laprevia.restobar.data.repository.UserPreferencesRepositoryImpl
import com.laprevia.restobar.domain.ProductManager
import com.laprevia.restobar.domain.repository.FirebaseProductRepository
import com.laprevia.restobar.domain.repository.UserPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(preferencesManager: PreferencesManager): UserPreferencesRepository {
        return UserPreferencesRepositoryImpl(preferencesManager)
    }

    @Provides
    @Singleton
    fun provideProductManager(
        firebaseProductRepository: FirebaseProductRepository
    ): ProductManager {
        return ProductManager(firebaseProductRepository)
    }

}
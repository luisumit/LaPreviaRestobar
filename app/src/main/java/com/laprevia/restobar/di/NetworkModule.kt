// di/NetworkModule.kt - VERSIÓN CORREGIDA CON LOS QUALIFIERS EXISTENTES
package com.laprevia.restobar.di

import com.laprevia.restobar.BuildConfig
import android.content.Context
import com.laprevia.restobar.data.remote.api.ApiService
import com.laprevia.restobar.data.remote.websocket.RealTimeWebSocketClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val EMULATOR_BASE_URL = BuildConfig.EMULATOR_BASE_URL
    private val PHYSICAL_DEVICE_BASE_URL = BuildConfig.PHYSICAL_DEVICE_BASE_URL
    private val EMULATOR_WS_URL = BuildConfig.EMULATOR_WS_URL
    private val PHYSICAL_DEVICE_WS_URL = BuildConfig.PHYSICAL_DEVICE_WS_URL

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    // 🔥 Determinar automáticamente si es emulador o dispositivo físico
    private fun isRunningOnEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic") ||
                android.os.Build.MODEL.contains("sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK"))
    }

    // 🔥 CORREGIDO: Con qualifier @BaseUrl (del mismo paquete)
    @Provides
    @Singleton
    @BaseUrl
    fun provideBaseUrl(): String {
        return if (isRunningOnEmulator()) {
            Timber.d("📱 Ejecutando en EMULADOR - URL: $EMULATOR_BASE_URL")
            EMULATOR_BASE_URL
        } else {
            Timber.d("📱 Ejecutando en DISPOSITIVO FÍSICO - URL: $PHYSICAL_DEVICE_BASE_URL")
            PHYSICAL_DEVICE_BASE_URL
        }
    }

    // 🔥 CORREGIDO: Con qualifier @WebSocketUrl (del mismo paquete)
    @Provides
    @Singleton
    @WebSocketUrl
    fun provideWebSocketUrl(): String {
        return if (isRunningOnEmulator()) {
            Timber.d("🔗 WebSocket URL para EMULADOR: $EMULATOR_WS_URL")
            EMULATOR_WS_URL
        } else {
            Timber.d("🔗 WebSocket URL para DISPOSITIVO FÍSICO: $PHYSICAL_DEVICE_WS_URL")
            PHYSICAL_DEVICE_WS_URL
        }
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        @BaseUrl baseUrl: String // 🔥 Usa el qualifier aquí
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    // 🔥 CORREGIDO: Usa el qualifier @WebSocketUrl
    @Provides
    @Singleton
    fun provideWebSocketClient(
        @ApplicationContext context: Context,
        @WebSocketUrl webSocketUrl: String // 🔥 Usa el qualifier aquí
    ): RealTimeWebSocketClient {
        Timber.d("🎯 Creando WebSocketClient para: $webSocketUrl")
        return RealTimeWebSocketClient(context, webSocketUrl)
    }
}

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

    // Ahora cada variant aporta sus propias URLs via BuildConfig
    private val BASE_URL = BuildConfig.BASE_URL
    private val PHYSICAL_BASE_URL = BuildConfig.PHYSICAL_BASE_URL
    private val WS_URL = BuildConfig.WS_URL
    private val PHYSICAL_WS_URL = BuildConfig.PHYSICAL_WS_URL

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingLevel = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE  // Sin logs en release
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = loggingLevel })
            .build()
    }

    private fun isRunningOnEmulator(): Boolean {
        return (android.os.Build.FINGERPRINT.startsWith("generic") ||
                android.os.Build.MODEL.contains("sdk") ||
                android.os.Build.MODEL.contains("Emulator") ||
                android.os.Build.MODEL.contains("Android SDK"))
    }

    @Provides
    @Singleton
    @BaseUrl
    fun provideBaseUrl(): String {
        return if (isRunningOnEmulator()) {
            Timber.d("📱 EMULADOR [${BuildConfig.ENVIRONMENT}] - URL: $BASE_URL")
            BASE_URL
        } else {
            Timber.d("📱 DISPOSITIVO FÍSICO [${BuildConfig.ENVIRONMENT}] - URL: $PHYSICAL_BASE_URL")
            PHYSICAL_BASE_URL
        }
    }

    @Provides
    @Singleton
    @WebSocketUrl
    fun provideWebSocketUrl(): String {
        return if (isRunningOnEmulator()) {
            Timber.d("🔗 WebSocket EMULADOR [${BuildConfig.ENVIRONMENT}]: $WS_URL")
            WS_URL
        } else {
            Timber.d("🔗 WebSocket FÍSICO [${BuildConfig.ENVIRONMENT}]: $PHYSICAL_WS_URL")
            PHYSICAL_WS_URL
        }
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        @BaseUrl baseUrl: String
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

    @Provides
    @Singleton
    fun provideWebSocketClient(
        @ApplicationContext context: Context,
        @WebSocketUrl webSocketUrl: String
    ): RealTimeWebSocketClient {
        Timber.d("🎯 Creando WebSocketClient [${BuildConfig.ENVIRONMENT}]: $webSocketUrl")
        return RealTimeWebSocketClient(context, webSocketUrl)
    }
}
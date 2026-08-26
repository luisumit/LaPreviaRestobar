package com.laprevia.restobar.di.koin

import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.laprevia.restobar.BuildConfig
import com.laprevia.restobar.data.local.datastore.PreferencesManager
import com.laprevia.restobar.data.local.db.AppDatabase
import com.laprevia.restobar.data.local.sync.SyncManager
import com.laprevia.restobar.data.printer.AutoPrintManager
import com.laprevia.restobar.data.printer.BluetoothPrinterManager
import com.laprevia.restobar.data.printer.ReceiptFormatter
import com.laprevia.restobar.data.remote.api.ApiService
import com.laprevia.restobar.data.remote.websocket.RealTimeWebSocketClient
import com.laprevia.restobar.data.repository.FirebaseInventoryRepositoryImpl
import com.laprevia.restobar.data.repository.FirebaseOrderRepositoryImpl
import com.laprevia.restobar.data.repository.FirebaseProductRepositoryImpl
import com.laprevia.restobar.data.repository.GitLiveProductRepository
import com.laprevia.restobar.data.repository.GitLiveTableRepository
import com.laprevia.restobar.data.repository.UserPreferencesRepositoryImpl
import com.laprevia.restobar.domain.ProductManager
import com.laprevia.restobar.domain.repository.FirebaseInventoryRepository
import com.laprevia.restobar.domain.repository.FirebaseOrderRepository
import com.laprevia.restobar.domain.repository.FirebaseProductRepository
import com.laprevia.restobar.domain.repository.FirebaseTableRepository
import com.laprevia.restobar.domain.repository.InventoryRepository
import com.laprevia.restobar.domain.repository.OrderRepository
import com.laprevia.restobar.domain.repository.ProductRepository
import com.laprevia.restobar.domain.repository.TableRepository
import com.laprevia.restobar.domain.repository.UserPreferencesRepository
import com.laprevia.restobar.domain.service.FirebaseInitializerService
import com.laprevia.restobar.domain.service.InventorySyncService
import com.laprevia.restobar.domain.usecase.CreateOrderUseCase
import com.laprevia.restobar.domain.usecase.CreateProductUseCase
import com.laprevia.restobar.domain.usecase.DeleteProductUseCase
import com.laprevia.restobar.domain.usecase.GetProductsUseCase
import com.laprevia.restobar.domain.usecase.GetTablesUseCase
import com.laprevia.restobar.domain.usecase.UpdateOrderStatusUseCase
import com.laprevia.restobar.domain.usecase.UpdateProductUseCase
import com.laprevia.restobar.presentation.viewmodel.AdminViewModel
import com.laprevia.restobar.presentation.viewmodel.ChefViewModel
import com.laprevia.restobar.presentation.viewmodel.InventoryViewModel
import com.laprevia.restobar.presentation.viewmodel.LoginViewModel
import com.laprevia.restobar.presentation.viewmodel.PrinterViewModel
import com.laprevia.restobar.presentation.viewmodel.SharedViewModel
import com.laprevia.restobar.presentation.viewmodel.SyncViewModel
import com.laprevia.restobar.presentation.viewmodel.WaiterViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Modulos Koin — espejo 1:1 del grafo Hilt (Fase 3 de la migracion KMP).
 * Los qualifiers propios de Hilt (@OrdersReference, @BaseUrl, ...) se convierten
 * en qualifiers named() de Koin. Coexisten con Hilt hasta completar el switch.
 */

// ---- Qualifiers (equivalentes a di/Qualifiers.kt) ----
val ordersRef = named("ordersRef")
val tablesRef = named("tablesRef")
val productsRef = named("productsRef")
val inventoryRef = named("inventoryRef")
val baseUrlQ = named("baseUrl")
val wsUrlQ = named("wsUrl")

private fun isRunningOnEmulator(): Boolean {
    return (android.os.Build.FINGERPRINT.startsWith("generic") ||
            android.os.Build.MODEL.contains("sdk") ||
            android.os.Build.MODEL.contains("Emulator") ||
            android.os.Build.MODEL.contains("Android SDK"))
}

/** Espejo de AppModule + DatabaseModule + managers de impresion/sync. */
val dataKoinModule = module {
    single { PreferencesManager(androidContext()) }
    single<UserPreferencesRepository> { UserPreferencesRepositoryImpl(get()) }
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, "restobar_db")
            .addMigrations(*AppDatabase.MIGRATIONS)
            .fallbackToDestructiveMigration()
            .build()
    }
    single { SyncManager(get(), get(), get(), get(), get()) }
    single { BluetoothPrinterManager(androidContext()) }
    factory { ReceiptFormatter() }
    single { AutoPrintManager(get(), get(), get()) }
}

/** Espejo de FirebaseModule + RepositoryModule (@Binds → bind). */
val firebaseKoinModule = module {
    single<FirebaseAuth> { Firebase.auth }
    single {
        FirebaseDatabase.getInstance().apply {
            try {
                setPersistenceEnabled(true)
            } catch (e: Exception) {
                // ya estaba configurado
            }
        }
    }
    single(ordersRef) { get<FirebaseDatabase>().getReference("orders") }
    single(tablesRef) { get<FirebaseDatabase>().getReference("tables") }
    single(productsRef) { get<FirebaseDatabase>().getReference("products") }
    single(inventoryRef) { get<FirebaseDatabase>().getReference("inventory") }

    single<FirebaseOrderRepository> {
        FirebaseOrderRepositoryImpl(get<DatabaseReference>(ordersRef))
    } bind OrderRepository::class
    // Mesas: implementacion MULTIPLATAFORMA (GitLive) desde el modulo shared — Fase 4
    single<FirebaseTableRepository> { GitLiveTableRepository() } bind TableRepository::class
    // Productos: implementacion MULTIPLATAFORMA (GitLive) desde el modulo shared — Fase 4
    single<FirebaseProductRepository> { GitLiveProductRepository() } bind ProductRepository::class
    single<FirebaseInventoryRepository> {
        FirebaseInventoryRepositoryImpl(get<DatabaseReference>(inventoryRef))
    } bind InventoryRepository::class

    single { FirebaseInitializerService(get(), get(), get(), get()) }
    single { InventorySyncService(get(), get()) }
}

/** Espejo de NetworkModule. */
val networkKoinModule = module {
    single {
        val loggingLevel = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply { level = loggingLevel })
            .build()
    }
    single(baseUrlQ) {
        if (isRunningOnEmulator()) {
            Timber.d("📱 EMULADOR [${BuildConfig.ENVIRONMENT}] - URL: ${BuildConfig.BASE_URL}")
            BuildConfig.BASE_URL
        } else {
            Timber.d("📱 DISPOSITIVO FÍSICO [${BuildConfig.ENVIRONMENT}] - URL: ${BuildConfig.PHYSICAL_BASE_URL}")
            BuildConfig.PHYSICAL_BASE_URL
        }
    }
    single(wsUrlQ) {
        if (isRunningOnEmulator()) BuildConfig.WS_URL else BuildConfig.PHYSICAL_WS_URL
    }
    single {
        Retrofit.Builder()
            .baseUrl(get<String>(baseUrlQ))
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    single<ApiService> { get<Retrofit>().create(ApiService::class.java) }
    single { RealTimeWebSocketClient(androidContext(), get(wsUrlQ)) }
}

/** Espejo de UseCaseModule + ProductManager. */
val domainKoinModule = module {
    single { ProductManager(get<FirebaseProductRepository>()) }
    single { CreateOrderUseCase(get<FirebaseOrderRepository>(), get<FirebaseTableRepository>()) }
    single { UpdateOrderStatusUseCase(get<FirebaseOrderRepository>()) }
    single { CreateProductUseCase(get<FirebaseProductRepository>()) }
    single { GetProductsUseCase(get<FirebaseProductRepository>()) }
    single { UpdateProductUseCase(get<FirebaseProductRepository>()) }
    single { DeleteProductUseCase(get<FirebaseProductRepository>()) }
    single { GetTablesUseCase(get<FirebaseTableRepository>()) }
}

/** Los 8 ViewModels (antes @HiltViewModel). */
val viewModelKoinModule = module {
    viewModel { AdminViewModel(get(), get(), get(), get(), get(), get(), androidContext()) }
    viewModel { WaiterViewModel(get(), get(), get(), get(), get(), get(), get(), androidContext()) }
    viewModel { ChefViewModel(get(), get(), get(), get(), get(), get(), androidContext()) }
    viewModel { PrinterViewModel(get(), get(), get()) }
    viewModel { InventoryViewModel(get(), get(), get(), get()) }
    viewModel { LoginViewModel(get()) }
    viewModel { SharedViewModel(get(), get()) }
    viewModel { SyncViewModel(get()) }
}

/** Lista completa para startKoin. */
val appKoinModules = listOf(
    dataKoinModule,
    firebaseKoinModule,
    networkKoinModule,
    domainKoinModule,
    viewModelKoinModule
)

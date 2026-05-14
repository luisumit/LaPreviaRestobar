package com.laprevia.restobar.presentation.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laprevia.restobar.data.local.db.AppDatabase
import com.laprevia.restobar.data.local.sync.SyncManager
import com.laprevia.restobar.data.mapper.toDomain
import com.laprevia.restobar.data.mapper.toEntity
import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.repository.FirebaseProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID
import kotlinx.coroutines.delay

data class AdminUiState(
    val products: List<Product> = emptyList(),
    val categories: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val warning: String? = null,
    val selectedProduct: Product? = null,
    val showProductForm: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isOffline: Boolean = false,
    val pendingSyncCount: Int = 0
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val firebaseProductRepository: FirebaseProductRepository,
    private val db: AppDatabase,
    private val syncManager: SyncManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val _isInternetAvailable = MutableStateFlow(true)
    val isInternetAvailable: StateFlow<Boolean> = _isInternetAvailable.asStateFlow()

    private fun checkInternet(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun startNetworkMonitoring() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                timber.log.Timber.d("🌐 INTERNET DISPONIBLE")
                _isInternetAvailable.value = true
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        isOffline = false,
                        warning = "🟢 Internet disponible - Sincronizando..."
                    )
                    loadProductsFromFirebase()
                    syncPendingProducts()
                    kotlinx.coroutines.delay(2000)
                    _uiState.value = _uiState.value.copy(warning = null)
                    if (_uiState.value.pendingSyncCount == 0) {
                        showMessage("✅ ¡Sincronización completada!", isSuccess = true)
                    }
                }
            }

            override fun onLost(network: Network) {
                timber.log.Timber.d("📱 SIN INTERNET")
                _isInternetAvailable.value = false
                viewModelScope.launch {
                    _uiState.value = _uiState.value.copy(
                        isOffline = true,
                        warning = "📱 SIN INTERNET - Los cambios se guardarán localmente",
                        error = null,
                        success = null
                    )
                }
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (_isInternetAvailable.value != hasInternet) {
                    _isInternetAvailable.value = hasInternet
                    if (hasInternet) {
                        viewModelScope.launch {
                            loadProductsFromFirebase()
                            syncPendingProducts()
                        }
                    }
                }
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

        _isInternetAvailable.value = checkInternet()
    }

    init {
        timber.log.Timber.d("🔧 AdminViewModel INICIADO - Offline-First con Room + Firebase")

        startNetworkMonitoring()

        viewModelScope.launch {
            loadProductsFromRoom()

            if (checkInternet()) {
                loadProductsFromFirebase()
                showMessage("🟢 Conectado a internet - Los datos se sincronizan automáticamente", isSuccess = true)
            } else {
                showMessage("📱 SIN INTERNET - Los cambios se guardarán localmente", isWarning = true)
            }
            checkPendingSync()

            // ✅ NUEVO: Escuchar cambios en tiempo real de Firebase
            listenToProductChanges()
        }
    }

    // ✅ NUEVO MÉTODO: Escuchar cambios en tiempo real
    private fun listenToProductChanges() {
        viewModelScope.launch {
            try {
                firebaseProductRepository.listenToProductChanges().collect { updatedProduct ->
                    timber.log.Timber.d("🔄 Admin: Cambio detectado en producto: ${updatedProduct.name}")
                    timber.log.Timber.d("   - Nuevo stock: ${updatedProduct.stock}")

                    // Actualizar en Room
                    val existing = db.productDao().getById(updatedProduct.id)
                    if (existing != null && existing.stock != updatedProduct.stock) {
                        db.productDao().insert(updatedProduct.toEntity().copy(syncStatus = "SYNCED"))
                        timber.log.Timber.d("✅ Admin: Stock actualizado en Room: ${updatedProduct.name} → ${updatedProduct.stock}")

                        // Refrescar UI
                        loadProductsFromRoom()
                        showMessage("📦 Stock actualizado: ${updatedProduct.name} = ${updatedProduct.stock}", isWarning = true)
                    }
                }
            } catch (e: Exception) {
                timber.log.Timber.d("❌ Admin: Error en listenToProductChanges: ${e.message}")
            }
        }
    }

    private fun showMessage(message: String, isError: Boolean = false, isSuccess: Boolean = false, isWarning: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            error = if (isError) message else null,
            success = if (isSuccess) message else null,
            warning = if (isWarning) message else null
        )

        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            clearError()
            clearSuccess()
            clearWarning()
        }
    }

    private suspend fun loadProductsFromRoom() {
        try {
            val roomProducts = db.productDao().getAll()
            val uniqueProducts = roomProducts
                .map { it.toDomain() }
                .distinctBy { it.id }
                .sortedBy { it.name }

            _uiState.value = _uiState.value.copy(
                products = uniqueProducts,
                categories = uniqueProducts.mapNotNull { it.category }.distinct().sorted(),
                isLoading = false,
                isOffline = !_isInternetAvailable.value
            )

            timber.log.Timber.d("📱 Admin: ${uniqueProducts.size} productos cargados desde Room")
            uniqueProducts.forEach { product ->
                if (product.trackInventory) {
                    timber.log.Timber.d("   - ${product.name}: stock=${product.stock}")
                }
            }
        } catch (e: Exception) {
            timber.log.Timber.d("❌ Admin: Error cargando desde Room: ${e.message}")
        }
    }

    private fun loadProductsFromFirebase() {
        viewModelScope.launch {
            try {
                timber.log.Timber.d("🔥 Admin: Cargando productos desde Firebase...")
                _uiState.value = _uiState.value.copy(isLoading = true)

                firebaseProductRepository.getProductsRealTime().collect { firebaseProducts ->
                    timber.log.Timber.d("✅ Productos desde Firebase: ${firebaseProducts.size}")
                    firebaseProducts.forEach { product ->
                        timber.log.Timber.d("   - ${product.name}: stock=${product.stock}")
                    }

                    firebaseProducts.forEach { product ->
                        val existing = db.productDao().getById(product.id)
                        if (existing == null || existing.stock != product.stock) {
                            db.productDao().insert(product.toEntity().copy(syncStatus = "SYNCED"))
                        }
                    }

                    val allRoomProducts = db.productDao().getAll().map { it.toDomain() }
                    val pendingProducts = db.productDao().getPending().map { it.toDomain() }

                    val pendingIds = pendingProducts.map { it.id }.toSet()
                    val syncedProducts = allRoomProducts.filter { it.id !in pendingIds }
                    val uniqueProducts = (syncedProducts + pendingProducts).distinctBy { it.id }.sortedBy { it.name }

                    _uiState.value = _uiState.value.copy(
                        products = uniqueProducts,
                        categories = uniqueProducts.mapNotNull { it.category }.distinct().sorted(),
                        isLoading = false,
                        isOffline = !_isInternetAvailable.value,
                        pendingSyncCount = pendingProducts.size
                    )

                    timber.log.Timber.d("📊 Admin: ${uniqueProducts.size} productos totales (${pendingProducts.size} pendientes)")
                }
            } catch (e: Exception) {
                timber.log.Timber.d("❌ Admin: Error cargando desde Firebase: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOffline = true
                )
                showMessage("Error de conexión: ${e.message}", isError = true)
            }
        }
    }

    private fun checkPendingSync() {
        viewModelScope.launch {
            try {
                val pendingCount = db.productDao().getPending().size
                _uiState.value = _uiState.value.copy(pendingSyncCount = pendingCount)

                if (pendingCount > 0 && _isInternetAvailable.value) {
                    syncPendingProducts()
                } else if (pendingCount > 0 && !_isInternetAvailable.value) {
                    showMessage("📱 ${pendingCount} producto(s) pendiente(s) de sincronizar", isWarning = true)
                }
            } catch (e: Exception) {
                timber.log.Timber.d("❌ Admin: Error verificando pendientes: ${e.message}")
            }
        }
    }

    private fun syncPendingProducts() {
        viewModelScope.launch {
            try {
                timber.log.Timber.d("🔄 Admin: Sincronizando productos pendientes...")
                showMessage("Sincronizando productos pendientes...", isWarning = true)

                syncManager.syncProducts()

                val pendingCount = db.productDao().getPending().size
                _uiState.value = _uiState.value.copy(pendingSyncCount = pendingCount)

                if (pendingCount == 0) {
                    showMessage("✅ ¡Sincronización completada! Todos los productos están en la nube", isSuccess = true)
                } else {
                    showMessage("⚠️ Quedan $pendingCount producto(s) pendiente(s)", isWarning = true)
                }

                loadProductsFromRoom()
                if (_isInternetAvailable.value) {
                    loadProductsFromFirebase()
                }
            } catch (e: Exception) {
                timber.log.Timber.d("❌ Admin: Error sincronizando: ${e.message}")
                showMessage("Error al sincronizar: ${e.message}", isError = true)
            }
        }
    }

    // ✅ NUEVO: Verificar stock bajo inmediatamente
    fun checkLowStockImmediately() {
        viewModelScope.launch {
            try {
                val products = db.productDao().getAll()
                val trackedProducts = products.filter { it.trackInventory }

                val outOfStock = trackedProducts.filter { it.stock == 0.0 }
                val lowStock = trackedProducts.filter { it.stock > 0 && it.stock <= it.minStock }

                if (outOfStock.isNotEmpty() || lowStock.isNotEmpty()) {
                    val message = when {
                        outOfStock.isNotEmpty() && lowStock.isNotEmpty() ->
                            "❌ ${outOfStock.size} agotados | ⚠️ ${lowStock.size} stock bajo"
                        outOfStock.isNotEmpty() ->
                            "❌ ${outOfStock.size} producto(s) AGOTADOS"
                        else ->
                            "⚠️ ${lowStock.size} producto(s) con stock bajo"
                    }
                    _uiState.value = _uiState.value.copy(warning = message)
                    println("⚠️ Admin: $message")
                }
            } catch (e: Exception) {
                println("❌ Admin: Error verificando stock bajo: ${e.message}")
            }
        }
    }

    fun showProductForm(product: Product? = null) {
        _uiState.value = _uiState.value.copy(
            showProductForm = true,
            selectedProduct = product,
            error = null,
            success = null,
            warning = null
        )
    }

    fun hideProductForm() {
        _uiState.value = _uiState.value.copy(
            showProductForm = false,
            selectedProduct = null
        )
    }

    fun showDeleteDialog(product: Product) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            selectedProduct = product
        )
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            selectedProduct = null
        )
    }

    fun createProduct(product: Product) {
        viewModelScope.launch {
            try {
                timber.log.Timber.d("📝 Admin: Creando producto - ${product.name}")
                _uiState.value = _uiState.value.copy(isLoading = true)

                val finalProduct = if (product.id.isEmpty()) {
                    product.copy(id = UUID.randomUUID().toString())
                } else {
                    product
                }

                val existing = db.productDao().getById(finalProduct.id)
                if (existing != null) {
                    showMessage("❌ Ya existe un producto con ese ID", isError = true)
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }

                db.productDao().insert(finalProduct.toEntity().copy(syncStatus = "PENDING"))
                timber.log.Timber.d("💾 Producto guardado en Room - ${finalProduct.name}")

                if (_isInternetAvailable.value) {
                    try {
                        firebaseProductRepository.createProduct(finalProduct)
                        db.productDao().updateStatus(finalProduct.id, "SYNCED")
                        showMessage("✅ Producto '${finalProduct.name}' creado y sincronizado", isSuccess = true)
                    } catch (e: Exception) {
                        showMessage("📱 Producto guardado LOCALMENTE. Se sincronizará después", isWarning = true)
                    }
                } else {
                    showMessage("📱 SIN INTERNET - Producto guardado LOCALMENTE", isWarning = true)
                }

                refreshProducts()
                hideProductForm()

                // ✅ Verificar stock después de crear
                checkLowStockImmediately()

            } catch (e: Exception) {
                timber.log.Timber.d("❌ Admin: Error creando producto: ${e.message}")
                showMessage("Error al crear producto: ${e.message}", isError = true)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            try {
                timber.log.Timber.d("📝 Admin: Actualizando producto - ${product.name}")
                _uiState.value = _uiState.value.copy(isLoading = true)

                val updatedEntity = product.toEntity().copy(
                    syncStatus = "PENDING",
                    version = System.currentTimeMillis(),
                    lastModified = System.currentTimeMillis()
                )
                db.productDao().insert(updatedEntity)
                timber.log.Timber.d("💾 Producto actualizado en Room - ${product.name}")

                if (_isInternetAvailable.value) {
                    try {
                        firebaseProductRepository.updateProduct(product)
                        db.productDao().updateStatus(product.id, "SYNCED")
                        showMessage("✅ Producto '${product.name}' actualizado y sincronizado", isSuccess = true)
                    } catch (e: Exception) {
                        showMessage("📱 Producto actualizado LOCALMENTE. Se sincronizará después", isWarning = true)
                    }
                } else {
                    showMessage("📱 SIN INTERNET - Producto actualizado LOCALMENTE", isWarning = true)
                }

                refreshProducts()
                hideProductForm()

                // ✅ Verificar stock después de actualizar
                checkLowStockImmediately()

            } catch (e: Exception) {
                timber.log.Timber.d("❌ Admin: Error actualizando producto: ${e.message}")
                showMessage("Error al actualizar producto: ${e.message}", isError = true)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun deleteProduct() {
        val product = _uiState.value.selectedProduct
        if (product == null) {
            showMessage("No se seleccionó ningún producto para eliminar", isError = true)
            return
        }

        viewModelScope.launch {
            try {
                timber.log.Timber.d("🗑️ Admin: Eliminando producto - ${product.name}")
                _uiState.value = _uiState.value.copy(isLoading = true)

                db.productDao().deleteProduct(product.id)
                timber.log.Timber.d("💾 Producto eliminado de Room - ${product.name}")

                if (_isInternetAvailable.value) {
                    try {
                        firebaseProductRepository.deleteProduct(product.id)
                        showMessage("✅ Producto '${product.name}' eliminado de la nube", isSuccess = true)
                    } catch (e: Exception) {
                        showMessage("📱 Producto eliminado LOCALMENTE. Se eliminará de la nube después", isWarning = true)
                    }
                } else {
                    showMessage("📱 SIN INTERNET - Producto eliminado LOCALMENTE", isWarning = true)
                }

                refreshProducts()
                hideDeleteDialog()

                // ✅ Verificar stock después de eliminar
                checkLowStockImmediately()

            } catch (e: Exception) {
                timber.log.Timber.d("❌ Admin: Error eliminando producto: ${e.message}")
                showMessage("Error al eliminar producto: ${e.message}", isError = true)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun manualSync() {
        viewModelScope.launch {
            try {
                timber.log.Timber.d("🔄 Admin: Sincronización manual...")
                _uiState.value = _uiState.value.copy(isLoading = true)

                if (_isInternetAvailable.value) {
                    syncManager.syncProducts()
                    syncManager.downloadProducts()
                    refreshProducts()

                    val pendingCount = db.productDao().getPending().size
                    if (pendingCount == 0) {
                        showMessage("✅ ¡Sincronización completada!", isSuccess = true)
                    } else {
                        showMessage("⚠️ Sincronización parcial. Quedan $pendingCount producto(s) pendiente(s)", isWarning = true)
                    }
                } else {
                    showMessage("❌ Sin conexión a internet. Los cambios se guardarán localmente", isError = true)
                }

            } catch (e: Exception) {
                timber.log.Timber.d("❌ Admin: Error en sincronización: ${e.message}")
                showMessage("Error en sincronización: ${e.message}", isError = true)
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun refreshProducts() {
        viewModelScope.launch {
            loadProductsFromRoom()
            if (_isInternetAvailable.value) {
                loadProductsFromFirebase()
            }
            checkPendingSync()
        }
    }

    fun clearError() { _uiState.value = _uiState.value.copy(error = null) }
    fun clearSuccess() { _uiState.value = _uiState.value.copy(success = null) }
    fun clearWarning() { _uiState.value = _uiState.value.copy(warning = null) }

    val hasPendingSync: Boolean get() = _uiState.value.pendingSyncCount > 0
    val connectionStatusText: String get() =
        if (!_isInternetAvailable.value) "🔴 SIN INTERNET - Modo offline"
        else if (hasPendingSync) "⏳ ${_uiState.value.pendingSyncCount} pendiente(s)"
        else "🟢 Conectado - Todo sincronizado"
}

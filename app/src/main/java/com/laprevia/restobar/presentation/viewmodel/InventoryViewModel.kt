package com.laprevia.restobar.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laprevia.restobar.data.local.db.AppDatabase
import com.laprevia.restobar.data.local.sync.SyncManager
import com.laprevia.restobar.data.mapper.toDomain
import com.laprevia.restobar.data.mapper.toEntity
import com.laprevia.restobar.data.model.Inventory
import com.laprevia.restobar.domain.repository.FirebaseInventoryRepository
import com.laprevia.restobar.domain.repository.FirebaseProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val firebaseInventoryRepository: FirebaseInventoryRepository,
    private val firebaseProductRepository: FirebaseProductRepository,  // ✅ AGREGADO
    private val db: AppDatabase,
    private val syncManager: SyncManager
) : ViewModel() {

    private val _inventory = MutableStateFlow<List<Inventory>>(emptyList())
    val inventory: StateFlow<List<Inventory>> = _inventory.asStateFlow()

    private val _lowStockItems = MutableStateFlow<List<Inventory>>(emptyList())
    val lowStockItems: StateFlow<List<Inventory>> = _lowStockItems.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    init {
        timber.log.Timber.d("🔧 InventoryViewModel INICIADO - Cargando productos desde Admin")

        viewModelScope.launch {
            // 1. Cargar desde productos (PRINCIPAL)
            loadInventoryFromProducts()

            // 2. Cargar items con stock bajo
            loadLowStockItems()

            // 3. Verificar pendientes
            checkPendingSync()
        }
    }

    // ==================== ✅ NUEVO: CARGAR DESDE PRODUCTOS (ADMIN) ====================

    private suspend fun loadInventoryFromProducts() {
        try {
            _isLoading.value = true
            timber.log.Timber.d("📦 Cargando productos desde Admin...")

            // Obtener productos activos con trackInventory = true
            firebaseProductRepository.getProductsWithInventory().collect { products ->
                val inventoryList = products.map { product ->
                    Inventory(
                        productId = product.id,
                        productName = product.name,
                        currentStock = product.stock,
                        unitOfMeasure = "unidades",
                        minimumStock = product.minStock,
                        category = product.category
                    )
                }

                _inventory.value = inventoryList
                _isLoading.value = false
                _isOffline.value = false
                _pendingSyncCount.value = 0

                timber.log.Timber.d("✅ Inventario cargado: ${inventoryList.size} productos desde Admin")

                if (inventoryList.isNotEmpty()) {
                    timber.log.Timber.d("📦 Detalles del inventario:")
                    inventoryList.forEachIndexed { index, item ->
                        timber.log.Timber.d("   ${index + 1}. ${item.productName}: ${item.currentStock} ${item.unitOfMeasure}")
                    }
                }
            }
        } catch (e: Exception) {
            timber.log.Timber.d("❌ Error cargando desde productos: ${e.message}")
            // Fallback: cargar desde Room
            loadInventoryFromRoom()
        }
    }

    // ==================== CARGA DESDE ROOM (FALLBACK) ====================

    private suspend fun loadInventoryFromRoom() {
        try {
            // Sincronizar productos desde Admin a Inventory local
            syncProductsFromAdmin()

            val roomInventory = db.inventoryDao().getAll()
            val inventoryList = roomInventory.map { it.toDomain() }

            _inventory.value = inventoryList
            _isOffline.value = true

            timber.log.Timber.d("📱 InventoryViewModel: ${inventoryList.size} items cargados desde Room (offline)")

            if (inventoryList.isEmpty()) {
                _errorMessage.value = "📱 No hay productos con inventario. Crea productos en el panel de Administrador."
            }
        } catch (e: Exception) {
            timber.log.Timber.d("❌ InventoryViewModel: Error cargando desde Room: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    // ==================== SINCRONIZAR PRODUCTOS DESDE ADMIN A INVENTORY LOCAL ====================

    private suspend fun syncProductsFromAdmin() {
        try {
            val products = db.productDao().getAll()
            val trackedProducts = products.filter { it.trackInventory }

            if (trackedProducts.isNotEmpty()) {
                timber.log.Timber.d("📦 InventoryViewModel: Sincronizando ${trackedProducts.size} productos desde Admin")

                trackedProducts.forEach { product ->
                    val existing = db.inventoryDao().getById(product.id)

                    if (existing == null) {
                        val newInventory = Inventory(
                            productId = product.id,
                            productName = product.name,
                            currentStock = product.stock,
                            unitOfMeasure = "unidades",
                            minimumStock = product.minStock,
                            category = product.category
                        )
                        db.inventoryDao().insert(newInventory.toEntity().copy(syncStatus = "SYNCED"))
                        timber.log.Timber.d("   ✅ Inventario creado para: ${product.name}")
                    } else if (existing.currentStock != product.stock || existing.productName != product.name) {
                        val updatedInventory = existing.copy(
                            productName = product.name,
                            currentStock = product.stock,
                            minimumStock = product.minStock,
                            category = product.category,
                            syncStatus = "SYNCED"
                        )
                        db.inventoryDao().insert(updatedInventory)
                        timber.log.Timber.d("   🔄 Inventario actualizado para: ${product.name}")
                    }
                }
            }
        } catch (e: Exception) {
            timber.log.Timber.d("❌ Error sincronizando productos desde Admin: ${e.message}")
        }
    }

    // ==================== STOCK BAJO ====================

    private fun loadLowStockItems() {
        viewModelScope.launch {
            try {
                // ✅ Obtener low stock desde productos
                firebaseProductRepository.getProductsWithInventory().collect { products ->
                    val lowStockProducts = products.filter { it.stock <= it.minStock && it.stock > 0 }

                    _lowStockItems.value = lowStockProducts.map { product ->
                        Inventory(
                            productId = product.id,
                            productName = product.name,
                            currentStock = product.stock,
                            unitOfMeasure = "unidades",
                            minimumStock = product.minStock,
                            category = product.category
                        )
                    }

                    timber.log.Timber.d("⚠️ InventoryViewModel: ${_lowStockItems.value.size} items con stock bajo")

                    if (_lowStockItems.value.isNotEmpty()) {
                        timber.log.Timber.d("🚨 Items con stock bajo:")
                        _lowStockItems.value.forEach { item ->
                            timber.log.Timber.d("   - ${item.productName}: ${item.currentStock}/${item.minimumStock} ${item.unitOfMeasure}")
                        }
                    }
                }
            } catch (e: Exception) {
                timber.log.Timber.d("❌ Error cargando items con stock bajo: ${e.message}")
                loadLowStockFromRoom()
            }
        }
    }

    private suspend fun loadLowStockFromRoom() {
        try {
            val allItems = db.inventoryDao().getAll().map { it.toDomain() }
            val lowStock = allItems.filter { it.currentStock <= it.minimumStock }
            _lowStockItems.value = lowStock
            timber.log.Timber.d("⚠️ InventoryViewModel: ${lowStock.size} items con stock bajo (desde Room)")
        } catch (e: Exception) {
            timber.log.Timber.d("❌ Error cargando low stock desde Room: ${e.message}")
        }
    }

    // ==================== VERIFICAR PENDIENTES ====================

    private fun checkPendingSync() {
        viewModelScope.launch {
            try {
                val pendingCount = db.inventoryDao().getPending().size
                _pendingSyncCount.value = pendingCount

                if (pendingCount > 0 && !_isOffline.value) {
                    syncPendingInventory()
                }
            } catch (e: Exception) {
                timber.log.Timber.d("❌ InventoryViewModel: Error verificando pendientes: ${e.message}")
            }
        }
    }

    private fun syncPendingInventory() {
        viewModelScope.launch {
            try {
                timber.log.Timber.d("🔄 InventoryViewModel: Sincronizando inventario pendiente...")
                syncManager.syncInventory()

                val pendingCount = db.inventoryDao().getPending().size
                _pendingSyncCount.value = pendingCount

                if (pendingCount == 0) {
                    _successMessage.value = "✅ Inventario sincronizado"
                }

                loadInventoryFromProducts()
            } catch (e: Exception) {
                timber.log.Timber.d("❌ InventoryViewModel: Error sincronizando: ${e.message}")
            }
        }
    }

    // ==================== FILTRADO ====================

    fun filterByCategory(category: String?) {
        timber.log.Timber.d("🎯 Filtrando por categoría: $category")
        _selectedCategory.value = category

        if (category == null) {
            viewModelScope.launch { loadInventoryFromProducts() }
        } else {
            val filtered = _inventory.value.filter {
                it.category?.equals(category, ignoreCase = true) == true
            }
            _inventory.value = filtered
            timber.log.Timber.d("🎯 Filtrado: ${filtered.size} items de categoría '$category'")
        }
    }

    fun getCategories(): List<String> {
        val categories = _inventory.value
            .mapNotNull { it.category }
            .distinct()
            .sorted()

        timber.log.Timber.d("🏷️ Categorías disponibles: $categories")
        return categories
    }

    // ==================== REFRESCAR ====================

    fun refreshInventory() {
        timber.log.Timber.d("🔄 Refrescando inventario...")
        viewModelScope.launch {
            loadInventoryFromProducts()
            loadLowStockItems()
            checkPendingSync()
        }
    }

    // ==================== ACTUALIZAR STOCK ====================

    fun updateStock(productId: String, newQuantity: Double) {
        viewModelScope.launch {
            try {
                timber.log.Timber.d("📊 Actualizando stock de producto $productId a $newQuantity")

                // 1️⃣ ACTUALIZAR EN FIREBASE PRODUCTS
                firebaseProductRepository.updateProductStock(productId, newQuantity)

                // 2️⃣ ACTUALIZAR EN INVENTORY
                firebaseInventoryRepository.updateStock(productId, newQuantity)

                // 3️⃣ ACTUALIZAR EN ROOM
                val product = db.productDao().getById(productId)
                if (product != null) {
                    val updatedProduct = product.copy(
                        stock = newQuantity,
                        syncStatus = "PENDING",
                        lastModified = System.currentTimeMillis()
                    )
                    db.productDao().insert(updatedProduct)

                    val inventoryItem = db.inventoryDao().getById(productId)
                    if (inventoryItem != null) {
                        val updatedInventory = inventoryItem.copy(
                            currentStock = newQuantity,
                            syncStatus = "PENDING",
                            lastModified = System.currentTimeMillis()
                        )
                        db.inventoryDao().insert(updatedInventory)
                    }
                }

                timber.log.Timber.d("✅ Stock actualizado correctamente")
                _successMessage.value = "✅ Stock actualizado"
                refreshInventory()

            } catch (e: Exception) {
                timber.log.Timber.d("❌ Error actualizando stock: ${e.message}")
                _errorMessage.value = "Error actualizando stock: ${e.message}"
            }
        }
    }

    // ==================== SINCRONIZACIÓN MANUAL ====================

    fun manualSync() {
        viewModelScope.launch {
            try {
                timber.log.Timber.d("🔄 InventoryViewModel: Sincronización manual...")
                _isLoading.value = true

                syncManager.syncInventory()
                syncManager.downloadInventory()
                syncProductsFromAdmin()
                loadInventoryFromProducts()

                val pendingCount = db.inventoryDao().getPending().size
                _successMessage.value = if (pendingCount == 0) {
                    "✅ Inventario sincronizado"
                } else {
                    "⚠️ Quedan $pendingCount items pendientes de sincronizar"
                }
                _isOffline.value = false

            } catch (e: Exception) {
                timber.log.Timber.d("❌ InventoryViewModel: Error en sincronización: ${e.message}")
                _errorMessage.value = "Error en sincronización: ${e.message}"
                _isOffline.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==================== UTILIDADES ====================

    fun getInventoryForProduct(productId: String): Inventory? {
        return _inventory.value.find { it.productId == productId }
    }

    fun isLowStock(productId: String): Boolean {
        return _lowStockItems.value.any { it.productId == productId }
    }

    fun clearSuccessMessage() { _successMessage.value = null }
    fun clearErrorMessage() { _errorMessage.value = null }

    // ==================== PROPIEDADES COMPUTADAS ====================

    val connectionStatusText: String get() =
        if (_isOffline.value) "🔴 Sin conexión - Modo offline"
        else if (_pendingSyncCount.value > 0) "⏳ Sincronizando (${_pendingSyncCount.value} pendientes)"
        else "🟢 Conectado"

    val hasPendingSync: Boolean get() = _pendingSyncCount.value > 0
}

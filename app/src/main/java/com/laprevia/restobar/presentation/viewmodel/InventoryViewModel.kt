// app/src/main/java/com/laprevia/restobar/presentation/viewmodel/InventoryViewModel.kt
package com.laprevia.restobar.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laprevia.restobar.data.model.Inventory
import com.laprevia.restobar.domain.repository.FirebaseInventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val firebaseInventoryRepository: FirebaseInventoryRepository
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

    init {
        println("🔧 InventoryViewModel inicializado - Conectado a Firebase")
        loadInventory()
        loadLowStockItems()
    }

    private fun loadInventory() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                firebaseInventoryRepository.getInventory().collect { inventoryList ->
                    println("✅ InventoryViewModel: ${inventoryList.size} items cargados desde Firebase")

                    if (inventoryList.isNotEmpty()) {
                        println("📦 Detalles del inventario cargado:")
                        inventoryList.forEachIndexed { index, item ->
                            println("   ${index + 1}. ${item.productName}")
                            println("      - ID: ${item.productId}")
                            println("      - Stock: ${item.currentStock} ${item.unitOfMeasure}")
                            println("      - Mínimo: ${item.minimumStock}")
                            println("      - Categoría: ${item.category ?: "Sin categoría"}")
                        }
                    } else {
                        println("📦 Inventario vacío - no hay items en Firebase")
                        println("💡 Usa el botón 'Cargar Datos de Ejemplo' para crear datos con stock real")
                    }

                    _inventory.value = inventoryList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _isLoading.value = false
                println("❌ Error cargando inventario desde Firebase: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun loadLowStockItems() {
        viewModelScope.launch {
            try {
                firebaseInventoryRepository.getLowStockItems().collect { lowStockList ->
                    println("⚠️ InventoryViewModel: ${lowStockList.size} items con stock bajo")

                    if (lowStockList.isNotEmpty()) {
                        println("🚨 Items con stock bajo:")
                        lowStockList.forEach { item ->
                            println("   - ${item.productName}: ${item.currentStock}/${item.minimumStock} ${item.unitOfMeasure}")
                        }
                    }

                    _lowStockItems.value = lowStockList
                }
            } catch (e: Exception) {
                println("❌ Error cargando items con stock bajo: ${e.message}")
            }
        }
    }

    fun filterByCategory(category: String?) {
        println("🎯 Filtrando por categoría: $category")
        _selectedCategory.value = category

        if (category == null) {
            loadInventory()
        } else {
            viewModelScope.launch {
                val allInventory = _inventory.value
                val filtered = allInventory.filter {
                    it.category?.equals(category, ignoreCase = true) == true
                }
                println("🎯 Filtrado: ${filtered.size} items de categoría '$category'")
                _inventory.value = filtered
            }
        }
    }

    fun getCategories(): List<String> {
        val categories = _inventory.value
            .mapNotNull { it.category }
            .distinct()
            .sorted()

        println("🏷️ Categorías disponibles: $categories")
        return categories
    }

    fun refreshInventory() {
        println("🔄 Refrescando inventario...")
        loadInventory()
        loadLowStockItems()
    }

    fun updateStock(productId: String, newQuantity: Double) {
        viewModelScope.launch {
            try {
                println("📊 Actualizando stock de producto $productId a $newQuantity")
                firebaseInventoryRepository.updateStock(productId, newQuantity)
                println("✅ Stock actualizado exitosamente")
                loadInventory()
            } catch (e: Exception) {
                println("❌ Error actualizando stock: ${e.message}")
            }
        }
    }

    fun getInventoryForProduct(productId: String): Inventory? {
        return _inventory.value.find { it.productId == productId }
    }

    fun isLowStock(productId: String): Boolean {
        return _lowStockItems.value.any { it.productId == productId }
    }

    // MÉTODO ACTUALIZADO - LIMPIAR COMPLETAMENTE EL INVENTARIO
    fun clearAllInventory() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                println("🗑️ InventoryViewModel: LIMPIANDO TODO EL INVENTARIO EXISTENTE...")

                // Obtener todos los productos actuales
                val currentInventory = _inventory.value
                println("📋 Productos a eliminar: ${currentInventory.size}")

                // Eliminar cada producto usando deleteProduct
                currentInventory.forEach { item ->
                    try {
                        firebaseInventoryRepository.deleteProduct(item.productId)
                        println("✅ Eliminado: ${item.productName} (${item.productId})")
                    } catch (e: Exception) {
                        println("❌ Error eliminando ${item.productId}: ${e.message}")
                    }
                    delay(50) // Pequeña pausa
                }

                _successMessage.value = "✅ Inventario limpiado. Ahora puedes cargar nuevos datos."
                println("✅ Limpieza completada")

                // Refrescar para verificar
                delay(1000)
                refreshInventory()

            } catch (e: Exception) {
                _errorMessage.value = "❌ Error limpiando inventario: ${e.message}"
                println("❌ Error en clearAllInventory: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // MÉTODO ACTUALIZADO - INVENTARIO PARA BAR CON MÁS LOGS
    fun initializeSampleData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                println("🔄 InventoryViewModel: Cargando INVENTARIO PARA BAR...")

                // PRIMERO VERIFICAR SI HAY DATOS EXISTENTES
                val currentCount = _inventory.value.size
                if (currentCount > 0) {
                    println("⚠️ Ya hay $currentCount productos en el inventario")
                    _errorMessage.value = "❌ Primero limpia el inventario existente"
                    _isLoading.value = false
                    return@launch
                }

                val sampleInventory = listOf(
                    // === LICORES PRINCIPALES ===
                    Inventory(
                        productId = "pisco_queirolo",
                        productName = "Pisco Queirolo Quebranta",
                        currentStock = 12.0,
                        unitOfMeasure = "botellas",
                        minimumStock = 3.0,
                        category = "Piscos"
                    ),
                    Inventory(
                        productId = "pisco_mosto_verde",
                        productName = "Pisco Mosto Verde",
                        currentStock = 8.0,
                        unitOfMeasure = "botellas",
                        minimumStock = 2.0,
                        category = "Piscos"
                    ),
                    Inventory(
                        productId = "ron_cartavio",
                        productName = "Ron Cartavio XO",
                        currentStock = 10.0,
                        unitOfMeasure = "botellas",
                        minimumStock = 2.0,
                        category = "Rones"
                    ),
                    Inventory(
                        productId = "whisky_johnnie",
                        productName = "Whisky Johnnie Walker",
                        currentStock = 6.0,
                        unitOfMeasure = "botellas",
                        minimumStock = 1.0,
                        category = "Whiskys"
                    ),
                    Inventory(
                        productId = "vodka_absolut",
                        productName = "Vodka Absolut",
                        currentStock = 8.0,
                        unitOfMeasure = "botellas",
                        minimumStock = 2.0,
                        category = "Vodkas"
                    ),
                    Inventory(
                        productId = "tequila_jose",
                        productName = "Tequila Jose Cuervo",
                        currentStock = 5.0,
                        unitOfMeasure = "botellas",
                        minimumStock = 1.0,
                        category = "Tequilas"
                    ),

                    // === CERVEZAS ===
                    Inventory(
                        productId = "cerveza_cusquena",
                        productName = "Cerveza Cusqueña Dorada",
                        currentStock = 144.0,
                        unitOfMeasure = "unidades",
                        minimumStock = 24.0,
                        category = "Cervezas"
                    ),
                    Inventory(
                        productId = "cerveza_cristal",
                        productName = "Cerveza Cristal",
                        currentStock = 120.0,
                        unitOfMeasure = "unidades",
                        minimumStock = 24.0,
                        category = "Cervezas"
                    ),
                    Inventory(
                        productId = "cerveza_heineken",
                        productName = "Cerveza Heineken",
                        currentStock = 96.0,
                        unitOfMeasure = "unidades",
                        minimumStock = 18.0,
                        category = "Cervezas"
                    ),
                    Inventory(
                        productId = "cerveza_corona",
                        productName = "Cerveza Corona",
                        currentStock = 84.0,
                        unitOfMeasure = "unidades",
                        minimumStock = 16.0,
                        category = "Cervezas"
                    ),

                    // === BEBIDAS NO ALCOHÓLICAS ===
                    Inventory(
                        productId = "coca_cola",
                        productName = "Coca Cola 500ml",
                        currentStock = 72.0,
                        unitOfMeasure = "unidades",
                        minimumStock = 12.0,
                        category = "Refrescos"
                    ),
                    Inventory(
                        productId = "inka_kola",
                        productName = "Inka Kola 500ml",
                        currentStock = 60.0,
                        unitOfMeasure = "unidades",
                        minimumStock = 12.0,
                        category = "Refrescos"
                    ),
                    Inventory(
                        productId = "agua_san_luis",
                        productName = "Agua San Luis 625ml",
                        currentStock = 48.0,
                        unitOfMeasure = "unidades",
                        minimumStock = 12.0,
                        category = "Aguas"
                    ),
                    Inventory(
                        productId = "ginger_ale",
                        productName = "Ginger Ale",
                        currentStock = 36.0,
                        unitOfMeasure = "unidades",
                        minimumStock = 8.0,
                        category = "Refrescos"
                    ),
                    Inventory(
                        productId = "agua_tonica",
                        productName = "Agua Tónica",
                        currentStock = 42.0,
                        unitOfMeasure = "unidades",
                        minimumStock = 10.0,
                        category = "Refrescos"
                    ),

                    // === JUGOS Y MIXERS ===
                    Inventory(
                        productId = "jugo_naranja",
                        productName = "Jugo de Naranja",
                        currentStock = 15.0,
                        unitOfMeasure = "litros",
                        minimumStock = 5.0,
                        category = "Jugos"
                    ),
                    Inventory(
                        productId = "jugo_pina",
                        productName = "Jugo de Piña",
                        currentStock = 12.0,
                        unitOfMeasure = "litros",
                        minimumStock = 4.0,
                        category = "Jugos"
                    ),
                    Inventory(
                        productId = "granadina",
                        productName = "Granadina",
                        currentStock = 8.0,
                        unitOfMeasure = "litros",
                        minimumStock = 2.0,
                        category = "Syrups"
                    ),
                    Inventory(
                        productId = "limonada",
                        productName = "Limonada",
                        currentStock = 10.0,
                        unitOfMeasure = "litros",
                        minimumStock = 3.0,
                        category = "Jugos"
                    ),

                    // === FRUTAS Y DECORACIONES ===
                    Inventory(
                        productId = "limon",
                        productName = "Limón",
                        currentStock = 8.0,
                        unitOfMeasure = "kg",
                        minimumStock = 3.0,
                        category = "Frutas"
                    ),
                    Inventory(
                        productId = "naranja",
                        productName = "Naranja",
                        currentStock = 6.0,
                        unitOfMeasure = "kg",
                        minimumStock = 2.0,
                        category = "Frutas"
                    ),
                    Inventory(
                        productId = "piña",
                        productName = "Piña",
                        currentStock = 5.0,
                        unitOfMeasure = "unidades",
                        minimumStock = 2.0,
                        category = "Frutas"
                    ),
                    Inventory(
                        productId = "hierbabuena",
                        productName = "Hierbabuena",
                        currentStock = 3.0,
                        unitOfMeasure = "paquetes",
                        minimumStock = 1.0,
                        category = "Hierbas"
                    ),
                    Inventory(
                        productId = "menta",
                        productName = "Menta fresca",
                        currentStock = 2.0,
                        unitOfMeasure = "paquetes",
                        minimumStock = 0.5,
                        category = "Hierbas"
                    ),

                    // === HIELO Y UTILERÍA ===
                    Inventory(
                        productId = "hielo",
                        productName = "Hielo en cubos",
                        currentStock = 50.0,
                        unitOfMeasure = "kg",
                        minimumStock = 15.0,
                        category = "Utilería"
                    ),
                    Inventory(
                        productId = "aceitunas",
                        productName = "Aceitunas verdes",
                        currentStock = 8.0,
                        unitOfMeasure = "kg",
                        minimumStock = 2.0,
                        category = "Aperitivos"
                    ),
                    Inventory(
                        productId = "queso",
                        productName = "Queso para tablas",
                        currentStock = 6.0,
                        unitOfMeasure = "kg",
                        minimumStock = 2.0,
                        category = "Aperitivos"
                    ),
                    Inventory(
                        productId = "salsa_tabasco",
                        productName = "Salsa Tabasco",
                        currentStock = 2.0,
                        unitOfMeasure = "litros",
                        minimumStock = 0.5,
                        category = "Condimentos"
                    ),
                    Inventory(
                        productId = "salsa_worcestershire",
                        productName = "Salsa Worcestershire",
                        currentStock = 1.5,
                        unitOfMeasure = "litros",
                        minimumStock = 0.3,
                        category = "Condimentos"
                    )
                )

                println("📝 Se van a crear ${sampleInventory.size} productos nuevos")

                // CREAR CADA PRODUCTO CON MÁS LOGS
                sampleInventory.forEachIndexed { index, inventoryItem ->
                    println("${index + 1}. Creando: ${inventoryItem.productName} (ID: ${inventoryItem.productId})")

                    val productData = mapOf<String, Any>(
                        "productName" to inventoryItem.productName,
                        "currentStock" to inventoryItem.currentStock,
                        "unitOfMeasure" to inventoryItem.unitOfMeasure,
                        "minimumStock" to inventoryItem.minimumStock,
                        "category" to (inventoryItem.category ?: "Sin categoría"),
                        "productId" to inventoryItem.productId // INCLUIR EL ID TAMBIÉN
                    )

                    println("   📤 Enviando a Firebase: $productData")

                    try {
                        firebaseInventoryRepository.updateInventoryFields(
                            inventoryItem.productId,
                            productData
                        )
                        println("   ✅ ${inventoryItem.productName} enviado a Firebase")
                    } catch (e: Exception) {
                        println("   ❌ Error con ${inventoryItem.productName}: ${e.message}")
                    }

                    // Pequeña pausa para no saturar Firebase
                    delay(100)
                }

                _successMessage.value = "✅ Inventario cargado: ${sampleInventory.size} productos PARA BAR"
                println("🎉 INVENTARIO DEL BAR CARGADO EXITOSAMENTE")

                // ESPERAR Y REFRESCAR
                delay(2000)
                println("🔄 Forzando refresh del inventario...")
                refreshInventory()

            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
                println("❌ InventoryViewModel: Error cargando inventario de bar: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Método para limpiar mensajes
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
// AdminViewModel.kt - VERSIÓN COMPLETA Y FUNCIONAL
package com.laprevia.restobar.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.repository.FirebaseProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val products: List<Product> = emptyList(),
    val categories: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedProduct: Product? = null,
    val showProductForm: Boolean = false,
    val showDeleteDialog: Boolean = false
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val firebaseProductRepository: FirebaseProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    init {
        println("🔧 AdminViewModel inicializado - Conectado a Firebase")
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                firebaseProductRepository.getProductsRealTime().collect { products ->
                    println("✅ Productos cargados desde Firebase: ${products.size}")
                    _uiState.value = _uiState.value.copy(
                        products = products,
                        categories = products.mapNotNull { it.category }.distinct(),
                        isLoading = false,
                        error = if (products.isEmpty()) "No hay productos registrados" else null
                    )
                }
            } catch (e: Exception) {
                println("❌ Error cargando productos desde Firebase: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = "Error al cargar productos: ${e.message}",
                    isLoading = false,
                    products = emptyList()
                )
            }
        }
    }

    fun showProductForm(product: Product? = null) {
        _uiState.value = _uiState.value.copy(
            showProductForm = true,
            selectedProduct = product
        )
    }

    fun hideProductForm() {
        _uiState.value = _uiState.value.copy(
            showProductForm = false,
            selectedProduct = null
        )
    }

    fun createProduct(product: Product) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                println("📝 Creando producto en Firebase: ${product.name}")
                firebaseProductRepository.createProduct(product)
                println("✅ Producto creado exitosamente: ${product.name}")

                hideProductForm()
                // No necesitamos recargar porque el listener en tiempo real actualizará automáticamente

            } catch (e: Exception) {
                println("❌ Error creando producto en Firebase: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = "Error al crear producto: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                println("📝 Actualizando producto en Firebase: ${product.name}")
                firebaseProductRepository.updateProduct(product)
                println("✅ Producto actualizado exitosamente: ${product.name}")

                hideProductForm()
                // No necesitamos recargar porque el listener en tiempo real actualizará automáticamente

            } catch (e: Exception) {
                println("❌ Error actualizando producto en Firebase: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = "Error al actualizar producto: ${e.message}",
                    isLoading = false
                )
            }
        }
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

    fun deleteProduct() {
        val product = _uiState.value.selectedProduct
        if (product != null) {
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                try {
                    println("🗑️ Eliminando producto de Firebase: ${product.name}")
                    firebaseProductRepository.deleteProduct(product.id)
                    println("✅ Producto eliminado exitosamente: ${product.name}")

                    hideDeleteDialog()
                    // No necesitamos recargar porque el listener en tiempo real actualizará automáticamente

                } catch (e: Exception) {
                    println("❌ Error eliminando producto de Firebase: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        error = "Error al eliminar producto: ${e.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun refreshProducts() {
        loadProducts()
    }
}
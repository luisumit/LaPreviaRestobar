package com.laprevia.restobar.domain.service

import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.repository.FirebaseProductRepository
import com.laprevia.restobar.domain.repository.FirebaseInventoryRepository
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventorySyncService @Inject constructor(
    private val productRepository: FirebaseProductRepository,
    private val inventoryRepository: FirebaseInventoryRepository
) {

    suspend fun startInventorySync() {
        Timber.i("🔄 InventorySyncService: Iniciando sincronización de inventario...")
        productRepository.getProductsRealTime()
            .distinctUntilChanged()
            .collect { products ->
                syncProductsToInventory(products)
            }
    }

    private suspend fun syncProductsToInventory(products: List<Product>) {
        try {
            Timber.d("📦 Sincronizando %d productos a inventario...", products.size)
            val productsWithInventory = products.filter { it.trackInventory && it.isActive }
            productsWithInventory.forEach { product ->
                syncProductToInventory(product)
            }
            cleanupOrphanedInventory(productsWithInventory)
        } catch (e: Exception) {
            Timber.e(e, "❌ Error crítico en sincronización de inventario")
        }
    }

    private suspend fun syncProductToInventory(product: Product) {
        try {
            val existingStock = inventoryRepository.getCurrentStock(product.id)
            if (existingStock == 0.0) {
                inventoryRepository.updateStock(product.id, product.stock)
                Timber.v("✅ Inventory creado: %s - Stock: %f", product.name, product.stock)
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error sincronizando %s", product.name)
        }
    }

    private suspend fun cleanupOrphanedInventory(validProducts: List<Product>) {
        try {
            val inventoryItems = inventoryRepository.getInventory().firstOrNull() ?: return
            val validProductIds = validProducts.map { it.id }.toSet()
            val orphanedItems = inventoryItems.filter { it.productId !in validProductIds }

            if (orphanedItems.isNotEmpty()) {
                Timber.d("🗑️ Eliminando %d items huérfanos del inventory", orphanedItems.size)
                orphanedItems.forEach { orphanedItem ->
                    try {
                        inventoryRepository.updateStock(orphanedItem.productId, 0.0)
                        Timber.v("   - Eliminado: %s", orphanedItem.productName)
                    } catch (e: Exception) {
                        Timber.e(e, "   ❌ Error eliminando %s", orphanedItem.productName)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error limpiando inventory huérfano")
        }
    }

    suspend fun checkInventoryConsistency() {
        try {
            Timber.d("🔍 Verificando consistencia de inventario...")
            val products = productRepository.getProductsRealTime().firstOrNull()
            val productsWithInventory = products?.filter { it.trackInventory && it.isActive } ?: emptyList()

            productsWithInventory.forEach { product ->
                try {
                    val inventoryStock = inventoryRepository.getCurrentStock(product.id)
                    if (inventoryStock != product.stock && inventoryStock == 0.0) {
                        inventoryRepository.updateStock(product.id, product.stock)
                        Timber.i("   ✅ Inventario corregido para %s", product.name)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "   ❌ Error verificando %s", product.name)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error en verificación de consistencia")
        }
    }
}

// domain/service/InventorySyncService.kt
package com.laprevia.restobar.domain.service

import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.repository.FirebaseProductRepository
import com.laprevia.restobar.domain.repository.FirebaseInventoryRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventorySyncService @Inject constructor(
    private val productRepository: FirebaseProductRepository,
    private val inventoryRepository: FirebaseInventoryRepository
) {

    suspend fun startInventorySync() {
        println("🔄 InventorySyncService: Iniciando sincronización de inventario...")

        productRepository.getProductsRealTime()
            .distinctUntilChanged()
            .collect { products ->
                syncProductsToInventory(products)
            }
    }

    private suspend fun syncProductsToInventory(products: List<Product>) {
        try {
            println("📦 Sincronizando ${products.size} productos a inventario...")

            val productsWithInventory = products.filter { it.trackInventory && it.isActive }
            println("🔄 ${productsWithInventory.size} productos requieren inventario")

            // Para cada producto que requiere inventario, crear/actualizar en inventory
            productsWithInventory.forEach { product ->
                syncProductToInventory(product)
            }

            // Eliminar productos del inventory que ya no existen o no requieren inventario
            cleanupOrphanedInventory(productsWithInventory)

        } catch (e: Exception) {
            println("❌ Error en sincronización: ${e.message}")
        }
    }

    private suspend fun syncProductToInventory(product: Product) {
        try {
            // Verificar si ya existe en inventory
            val existingStock = inventoryRepository.getCurrentStock(product.id)

            if (existingStock == 0.0) {
                // Crear nuevo registro de inventory usando stock del producto
                val inventoryItem = com.laprevia.restobar.data.model.Inventory(
                    productId = product.id,
                    productName = product.name,
                    currentStock = product.stock,
                    unitOfMeasure = "unidades",
                    minimumStock = product.minStock,
                    category = product.category
                )

                // Guardar en Firebase
                inventoryRepository.updateStock(product.id, inventoryItem.currentStock)
                println("✅ Inventory creado: ${product.name} - Stock: ${product.stock}")
            } else {
                // Solo actualizar nombre y categoría si es necesario
                println("ℹ️ Inventory existente: ${product.name} - Stock: $existingStock")
            }

        } catch (e: Exception) {
            println("❌ Error sincronizando ${product.name}: ${e.message}")
        }
    }

    private suspend fun cleanupOrphanedInventory(validProducts: List<Product>) {
        try {
            // Obtener todos los items del inventory
            val allInventory = inventoryRepository.getInventory()

            // Crear un flujo para procesar el inventory
            allInventory.collect { inventoryItems ->
                val validProductIds = validProducts.map { it.id }.toSet()

                // Encontrar items huérfanos (que no están en productos válidos)
                val orphanedItems = inventoryItems.filter { it.productId !in validProductIds }

                if (orphanedItems.isNotEmpty()) {
                    println("🗑️ Eliminando ${orphanedItems.size} items huérfanos del inventory")
                    orphanedItems.forEach { orphanedItem ->
                        try {
                            inventoryRepository.updateStock(orphanedItem.productId, 0.0)
                            println("   - Eliminado: ${orphanedItem.productName}")
                        } catch (e: Exception) {
                            println("   ❌ Error eliminando ${orphanedItem.productName}: ${e.message}")
                        }
                    }
                }
            }

        } catch (e: Exception) {
            println("❌ Error limpiando inventory huérfano: ${e.message}")
        }
    }

    // Método para actualizar inventario cuando cambia un producto
    suspend fun updateInventoryForProduct(product: Product) {
        try {
            if (product.trackInventory && product.isActive) {
                println("🔄 Actualizando inventario para: ${product.name}")
                syncProductToInventory(product)
            } else {
                // Si el producto ya no requiere inventario, limpiar
                inventoryRepository.updateStock(product.id, 0.0)
                println("🗑️ Producto removido del inventario: ${product.name}")
            }
        } catch (e: Exception) {
            println("❌ Error actualizando inventario para ${product.name}: ${e.message}")
        }
    }

    // Método para verificar consistencia entre productos e inventario
    suspend fun checkInventoryConsistency() {
        try {
            println("🔍 Verificando consistencia de inventario...")

            // Usar firstOrNull() estándar de coroutines flow
            val products = productRepository.getProductsRealTime().firstOrNull()
            val productsWithInventory = products?.filter { it.trackInventory && it.isActive } ?: emptyList()

            println("📊 Productos que deberían tener inventario: ${productsWithInventory.size}")

            productsWithInventory.forEach { product ->
                try {
                    val inventoryStock = inventoryRepository.getCurrentStock(product.id)
                    val productStock = product.stock

                    if (inventoryStock != productStock) {
                        println("⚠️  Inconsistencia encontrada: ${product.name}")
                        println("   - Stock en producto: $productStock")
                        println("   - Stock en inventario: $inventoryStock")

                        // Corregir la inconsistencia
                        if (inventoryStock == 0.0) {
                            inventoryRepository.updateStock(product.id, productStock)
                            println("   ✅ Inventario corregido: $productStock")
                        }
                    }
                } catch (e: Exception) {
                    println("   ❌ Error verificando ${product.name}: ${e.message}")
                }
            }

            println("✅ Verificación de consistencia completada")

        } catch (e: Exception) {
            println("❌ Error en verificación de consistencia: ${e.message}")
        }
    }

    // Método para obtener productos con bajo stock
    suspend fun getLowStockProducts(): List<Product> {
        return try {
            val products = productRepository.getProductsRealTime().firstOrNull() ?: emptyList()
            val lowStockProducts = mutableListOf<Product>()

            products.filter { it.trackInventory && it.isActive }.forEach { product ->
                try {
                    val inventoryStock = inventoryRepository.getCurrentStock(product.id)
                    if (inventoryStock <= product.minStock) {
                        lowStockProducts.add(product)
                    }
                } catch (e: Exception) {
                    // Ignorar errores individuales
                }
            }

            println("📉 Productos con stock bajo: ${lowStockProducts.size}")
            lowStockProducts
        } catch (e: Exception) {
            println("❌ Error obteniendo productos con stock bajo: ${e.message}")
            emptyList()
        }
    }

    // Método para forzar sincronización completa
    suspend fun forceFullSync() {
        try {
            println("🔄 Forzando sincronización completa de inventario...")

            val products = productRepository.getProductsRealTime().firstOrNull() ?: emptyList()
            syncProductsToInventory(products)

            println("✅ Sincronización completa finalizada")
        } catch (e: Exception) {
            println("❌ Error en sincronización completa: ${e.message}")
        }
    }
}
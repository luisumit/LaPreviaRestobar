package com.laprevia.restobar.domain.service

import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.data.model.TableStatus
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.domain.repository.FirebaseTableRepository
import com.laprevia.restobar.domain.repository.FirebaseProductRepository
import com.laprevia.restobar.domain.repository.FirebaseOrderRepository
import com.laprevia.restobar.domain.repository.FirebaseInventoryRepository
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseInitializerService @Inject constructor(
    private val tableRepository: FirebaseTableRepository,
    private val productRepository: FirebaseProductRepository,
    private val orderRepository: FirebaseOrderRepository,
    private val inventoryRepository: FirebaseInventoryRepository
) {

    suspend fun initializeAllData() {
        Timber.i("🚀 FirebaseInitializerService: Iniciando inicialización completa...")
        try {
            initializeDefaultTables()
            initializeDefaultProducts()
            initializeDefaultInventory()
            checkActiveOrders()
            Timber.i("🎉 Firebase completamente inicializado")
        } catch (e: Exception) {
            Timber.e(e, "💥 Error en inicialización completa")
        }
    }

    private suspend fun initializeDefaultTables() {
        try {
            val existingTables = tableRepository.getTablesRealTime().firstOrNull()
            if (existingTables.isNullOrEmpty()) {
                Timber.d("📝 Creando mesas por defecto...")
                val defaultTables = listOf(
                    Table(id = 1, number = 1, status = TableStatus.LIBRE, capacity = 4),
                    Table(id = 2, number = 2, status = TableStatus.LIBRE, capacity = 4),
                    Table(id = 3, number = 3, status = TableStatus.LIBRE, capacity = 6),
                    Table(id = 4, number = 4, status = TableStatus.LIBRE, capacity = 2),
                    Table(id = 5, number = 5, status = TableStatus.LIBRE, capacity = 8)
                )
                defaultTables.forEach { table ->
                    tableRepository.updateTable(table)
                    Timber.v("   - Mesa creada: %d", table.number)
                }
            } else {
                Timber.i("ℹ️ Ya existen %d mesas", existingTables.size)
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error inicializando mesas")
        }
    }

    private suspend fun initializeDefaultProducts() {
        try {
            val existingProducts = productRepository.getProductsRealTime().firstOrNull()
            if (existingProducts.isNullOrEmpty()) {
                Timber.d("📝 Creando productos por defecto...")
                val currentTime = System.currentTimeMillis()
                val defaultProducts = listOf(
                    Product(
                        id = UUID.randomUUID().toString(),
                        name = "Cerveza Artesanal",
                        description = "Cerveza rubia artesanal de barril",
                        category = "Bebidas",
                        salePrice = 12.0,
                        costPrice = 6.0,
                        trackInventory = true,
                        stock = 50.0,
                        minStock = 10.0,
                        imageUrl = null,
                        isActive = true,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    ),
                    Product(
                        id = UUID.randomUUID().toString(),
                        name = "Pisco Sour",
                        description = "Coctel tradicional peruano",
                        category = "Cocteles",
                        salePrice = 18.0,
                        costPrice = 8.0,
                        trackInventory = true,
                        stock = 30.0,
                        minStock = 5.0,
                        imageUrl = null,
                        isActive = true,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    )
                )
                defaultProducts.forEach { product ->
                    productRepository.createProduct(product)
                    Timber.v("   - Producto creado: %s", product.name)
                }
            } else {
                Timber.i("ℹ️ Ya existen %d productos", existingProducts.size)
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error verificando productos")
        }
    }

    private suspend fun initializeDefaultInventory() {
        try {
            Timber.d("📦 Verificando inventario...")
            val products = productRepository.getProductsRealTime().firstOrNull()
            val productsWithInventory = products?.filter { it.trackInventory } ?: emptyList()

            if (productsWithInventory.isNotEmpty()) {
                Timber.i("ℹ️ %d productos requieren control de inventario", productsWithInventory.size)
                productsWithInventory.forEach { product ->
                    try {
                        val currentStock = inventoryRepository.getCurrentStock(product.id)
                        if (currentStock == 0.0) {
                            inventoryRepository.updateStock(product.id, product.stock)
                            Timber.v("   - Stock inicializado para: %s - %f", product.name, product.stock)
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "   ⚠️ Error con %s", product.name)
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error inicializando inventario")
        }
    }

    private suspend fun checkActiveOrders() {
        try {
            val activeOrders = orderRepository.getActiveOrders().firstOrNull()
            Timber.i("📊 Órdenes activas: %d", activeOrders?.size ?: 0)
        } catch (e: Exception) {
            Timber.e(e, "⚠️ Error verificando órdenes")
        }
    }

    suspend fun checkFirebaseStatus() {
        Timber.d("🔍 Verificando estado de Firebase...")
        try {
            val tables = tableRepository.getTablesRealTime().firstOrNull()
            val products = productRepository.getProductsRealTime().firstOrNull()
            val orders = orderRepository.getActiveOrders().firstOrNull()
            Timber.i("   Estado: %d mesas, %d productos, %d órdenes activas", 
                tables?.size ?: 0, products?.size ?: 0, orders?.size ?: 0)
        } catch (e: Exception) {
            Timber.e(e, "❌ Error verificando estado")
        }
    }

    suspend fun createSampleOrder() {
        try {
            Timber.d("📝 Creando orden de ejemplo...")
            val products = productRepository.getProductsRealTime().firstOrNull()
            val sampleProducts = products?.take(2) ?: return

            if (sampleProducts.size >= 2) {
                val orderItems = sampleProducts.mapIndexed { index, product ->
                    OrderItem(product = product, quantity = index + 1)
                }
                val sampleOrder = Order(
                    tableId = 1,
                    tableNumber = 1,
                    items = orderItems,
                    status = OrderStatus.ENVIADO,
                    total = orderItems.sumOf { it.subtotal }
                )
                orderRepository.createOrder(sampleOrder)
                Timber.i("✅ Orden de ejemplo creada")
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error creando orden de ejemplo")
        }
    }
}
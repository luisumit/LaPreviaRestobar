// app/src/main/java/com/laprevia/restobar/domain/service/FirebaseInitializerService.kt
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
        println("🚀 FirebaseInitializerService: Iniciando inicialización completa...")

        try {
            // 1. Inicializar mesas si no existen
            initializeDefaultTables()
            println("✅ Mesas inicializadas")

            // 2. Verificar y crear productos si no existen
            initializeDefaultProducts()
            println("✅ Productos verificados")

            // 3. Inicializar inventario para productos que lo requieren
            initializeDefaultInventory()
            println("✅ Inventario verificado")

            // 4. Verificar órdenes activas
            checkActiveOrders()
            println("✅ Estado de órdenes verificado")

            println("🎉 Firebase completamente inicializado")

        } catch (e: Exception) {
            println("💥 Error en inicialización completa: ${e.message}")
            e.printStackTrace()
        }
    }

    private suspend fun initializeDefaultTables() {
        try {
            val existingTables = tableRepository.getTablesRealTime().firstOrNull()

            if (existingTables.isNullOrEmpty()) {
                println("📝 Creando mesas por defecto...")

                val defaultTables = listOf(
                    Table(
                        id = 1,
                        number = 1,
                        status = TableStatus.LIBRE,
                        capacity = 4
                    ),
                    Table(
                        id = 2,
                        number = 2,
                        status = TableStatus.LIBRE,
                        capacity = 4
                    ),
                    Table(
                        id = 3,
                        number = 3,
                        status = TableStatus.LIBRE,
                        capacity = 6
                    ),
                    Table(
                        id = 4,
                        number = 4,
                        status = TableStatus.LIBRE,
                        capacity = 2
                    ),
                    Table(
                        id = 5,
                        number = 5,
                        status = TableStatus.LIBRE,
                        capacity = 8
                    )
                )

                defaultTables.forEach { table ->
                    tableRepository.updateTable(table)
                    println("   - Mesa creada: ${table.number}")
                }
            } else {
                println("ℹ️ Ya existen ${existingTables.size} mesas")
                existingTables.take(3).forEach { table ->
                    println("   - Mesa ${table.number}: ${table.status} (Capacidad: ${table.capacity})")
                }
            }
        } catch (e: Exception) {
            println("❌ Error inicializando mesas: ${e.message}")
        }
    }

    private suspend fun initializeDefaultProducts() {
        try {
            val existingProducts = productRepository.getProductsRealTime().firstOrNull()

            if (existingProducts.isNullOrEmpty()) {
                println("📝 Creando productos por defecto...")

                val currentTime = System.currentTimeMillis()
                val defaultProducts = listOf(
                    // Bebidas
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
                    ),
                    Product(
                        id = UUID.randomUUID().toString(),
                        name = "Agua Mineral",
                        description = "Agua mineral 500ml",
                        category = "Bebidas",
                        salePrice = 5.0,
                        costPrice = 2.0,
                        trackInventory = true,
                        stock = 100.0,
                        minStock = 20.0,
                        imageUrl = null,
                        isActive = true,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    ),
                    // Platos principales
                    Product(
                        id = UUID.randomUUID().toString(),
                        name = "Lomo Saltado",
                        description = "Plato tradicional peruano con lomo, papas y arroz",
                        category = "Platos Principales",
                        salePrice = 35.0,
                        costPrice = 15.0,
                        trackInventory = false,
                        stock = 0.0,
                        minStock = 0.0,
                        imageUrl = null,
                        isActive = true,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    ),
                    Product(
                        id = UUID.randomUUID().toString(),
                        name = "Ceviche Mixto",
                        description = "Ceviche de pescado y mariscos",
                        category = "Entradas",
                        salePrice = 40.0,
                        costPrice = 18.0,
                        trackInventory = false,
                        stock = 0.0,
                        minStock = 0.0,
                        imageUrl = null,
                        isActive = true,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    ),
                    Product(
                        id = UUID.randomUUID().toString(),
                        name = "Papas Fritas",
                        description = "Porción de papas fritas con salsa",
                        category = "Snacks",
                        salePrice = 15.0,
                        costPrice = 5.0,
                        trackInventory = true,
                        stock = 20.0,
                        minStock = 5.0,
                        imageUrl = null,
                        isActive = true,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    ),
                    // Postres
                    Product(
                        id = UUID.randomUUID().toString(),
                        name = "Suspiro Limeño",
                        description = "Postre tradicional de manjar blanco y merengue",
                        category = "Postres",
                        salePrice = 12.0,
                        costPrice = 4.0,
                        trackInventory = false,
                        stock = 0.0,
                        minStock = 0.0,
                        imageUrl = null,
                        isActive = true,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    ),
                    Product(
                        id = UUID.randomUUID().toString(),
                        name = "Mazamorra Morada",
                        description = "Postre de maíz morado con frutas",
                        category = "Postres",
                        salePrice = 10.0,
                        costPrice = 3.0,
                        trackInventory = false,
                        stock = 0.0,
                        minStock = 0.0,
                        imageUrl = null,
                        isActive = true,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    )
                )

                defaultProducts.forEach { product ->
                    productRepository.createProduct(product)
                    println("   - Producto creado: ${product.name} (S/. ${product.salePrice ?: 0.0})")
                }
            } else {
                println("ℹ️ Ya existen ${existingProducts.size} productos")
                existingProducts.take(3).forEach { product ->
                    println("   - ${product.name}: S/. ${product.salePrice ?: 0.0}")
                    println("     Categoría: ${product.category}")
                    if (product.trackInventory) {
                        println("     Stock: ${product.stock}")
                    }
                }
            }
        } catch (e: Exception) {
            println("❌ Error verificando productos: ${e.message}")
        }
    }

    private suspend fun initializeDefaultInventory() {
        try {
            println("📦 Verificando inventario...")

            // Obtener productos que requieren control de inventario
            val products = productRepository.getProductsRealTime().firstOrNull()
            val productsWithInventory = products?.filter { it.trackInventory } ?: emptyList()

            if (productsWithInventory.isNotEmpty()) {
                println("ℹ️ ${productsWithInventory.size} productos requieren control de inventario")

                productsWithInventory.forEach { product ->
                    try {
                        // Verificar si ya existe stock para este producto
                        val currentStock = inventoryRepository.getCurrentStock(product.id)

                        if (currentStock == 0.0) {
                            // Inicializar stock con la cantidad del producto
                            inventoryRepository.updateStock(product.id, product.stock)
                            println("   - Stock inicializado para: ${product.name} - ${product.stock}")
                        } else {
                            println("   - Stock existente para: ${product.name} - $currentStock")
                        }
                    } catch (e: Exception) {
                        println("   ⚠️ Error con ${product.name}: ${e.message}")
                    }
                }
            } else {
                println("ℹ️ No hay productos que requieran control de inventario")
            }

        } catch (e: Exception) {
            println("❌ Error inicializando inventario: ${e.message}")
        }
    }

    private suspend fun checkActiveOrders() {
        try {
            val activeOrders = orderRepository.getActiveOrders().firstOrNull()
            println("📊 Órdenes activas: ${activeOrders?.size ?: 0}")

            activeOrders?.take(2)?.forEach { order ->
                println("   - Orden ${order.id}: ${order.status}")
                println("     Mesa: ${order.tableNumber} - Total: S/. ${order.total}")
                // ✅ ACTUALIZADO: Usar productName en lugar de product.name
                order.items.take(2).forEach { item ->
                    println("       • ${item.quantity}x ${item.productName}")
                }
            }
        } catch (e: Exception) {
            println("⚠️ Error verificando órdenes: ${e.message}")
        }
    }

    // Método para verificar el estado general de Firebase
    suspend fun checkFirebaseStatus() {
        println("🔍 Verificando estado de Firebase...")

        try {
            // Verificar mesas
            val tables = tableRepository.getTablesRealTime().firstOrNull()
            println("   Mesas: ${tables?.size ?: 0}")
            tables?.take(3)?.forEach { table ->
                println("     - Mesa ${table.number}: ${table.status} (Capacidad: ${table.capacity})")
            }

            // Verificar productos
            val products = productRepository.getProductsRealTime().firstOrNull()
            println("   Productos: ${products?.size ?: 0}")

            // Verificar productos con inventario
            val productsWithInventory = products?.filter { it.trackInventory }
            println("   Productos con inventario: ${productsWithInventory?.size ?: 0}")

            // Verificar órdenes activas
            val orders = orderRepository.getActiveOrders().firstOrNull()
            println("   Órdenes activas: ${orders?.size ?: 0}")

            println("✅ Estado de Firebase verificado")

        } catch (e: Exception) {
            println("❌ Error verificando estado: ${e.message}")
        }
    }

    // Método para forzar reinicialización
    suspend fun forceReinitialize() {
        println("🔄 FirebaseInitializerService: Forzando reinicialización...")

        // Recrear mesas
        initializeDefaultTables()

        // Recrear productos
        initializeDefaultProducts()

        // Recrear inventario
        initializeDefaultInventory()

        println("✅ Reinicialización forzada completada")
    }

    // Método adicional: Verificar productos que necesitan inventario
    suspend fun checkProductsNeedingInventory() {
        try {
            val products = productRepository.getProductsRealTime().firstOrNull()
            val productsWithInventory = products?.filter { it.trackInventory }

            println("📋 Productos que requieren control de inventario: ${productsWithInventory?.size ?: 0}")
            productsWithInventory?.forEach { product ->
                val currentStock = inventoryRepository.getCurrentStock(product.id)
                println("   - ${product.name}: $currentStock/${product.stock} (Mín: ${product.minStock})")
            }
        } catch (e: Exception) {
            println("⚠️ Error verificando productos con inventario: ${e.message}")
        }
    }

    // ✅ ACTUALIZADO: Método para crear una orden de ejemplo usando la nueva estructura
    suspend fun createSampleOrder() {
        try {
            println("📝 Creando orden de ejemplo...")

            // Obtener productos existentes
            val products = productRepository.getProductsRealTime().firstOrNull()
            val sampleProducts = products?.take(2) ?: return

            if (sampleProducts.size >= 2) {
                // ✅ ACTUALIZADO: Usar el nuevo constructor de OrderItem con campos planos
                val orderItems = sampleProducts.mapIndexed { index, product ->
                    OrderItem(
                        product = product, // Esto usará el nuevo constructor que crea campos planos
                        quantity = index + 1
                    )
                }

                val sampleOrder = Order(
                    tableId = 1,
                    tableNumber = 1,
                    items = orderItems,
                    status = OrderStatus.ENVIADO,
                    total = orderItems.sumOf { it.subtotal }
                )

                orderRepository.createOrder(sampleOrder)
                println("✅ Orden de ejemplo creada en Mesa 1")

                // ✅ LOG DETALLADO DE LA ORDEN DE EJEMPLO
                println("📋 Detalles de la orden de ejemplo:")
                println("   - Mesa: 1")
                println("   - Items: ${orderItems.size}")
                orderItems.forEach { item ->
                    println("     • ${item.quantity}x ${item.productName} - S/. ${item.subtotal}")
                    println("       (ID: ${item.productId}, Precio: ${item.unitPrice})")
                }
                println("   - Total: S/. ${sampleOrder.total}")
            }
        } catch (e: Exception) {
            println("❌ Error creando orden de ejemplo: ${e.message}")
        }
    }

    // Método para limpiar datos de prueba (útil para desarrollo)
    suspend fun cleanupTestData() {
        try {
            println("🧹 Limpiando datos de prueba...")

            // Obtener todas las órdenes activas
            val activeOrders = orderRepository.getActiveOrders().firstOrNull()
            activeOrders?.forEach { order ->
                orderRepository.deleteOrder(order.id)
                println("   - Orden eliminada: ${order.id}")
            }

            // Limpiar mesas (liberar todas)
            val tables = tableRepository.getTablesRealTime().firstOrNull()
            tables?.forEach { table ->
                if (table.status != TableStatus.LIBRE) {
                    tableRepository.clearTable(table.id)
                    println("   - Mesa liberada: ${table.number}")
                }
            }

            println("✅ Datos de prueba limpiados")
        } catch (e: Exception) {
            println("❌ Error limpiando datos: ${e.message}")
        }
    }

    // ✅ NUEVO MÉTODO: Verificar estructura de órdenes existentes
    suspend fun checkOrderStructure() {
        try {
            println("🔍 Verificando estructura de órdenes...")

            val orders = orderRepository.getActiveOrders().firstOrNull()
            println("📊 Total de órdenes: ${orders?.size ?: 0}")

            orders?.forEachIndexed { index, order ->
                println("   Orden ${index + 1}:")
                println("     - ID: ${order.id}")
                println("     - Mesa: ${order.tableNumber}")
                println("     - Estado: ${order.status}")
                println("     - Items: ${order.items.size}")

                order.items.forEachIndexed { itemIndex, item ->
                    println("       Item ${itemIndex + 1}:")
                    println("         - ProductId: ${item.productId}")
                    println("         - ProductName: ${item.productName}")
                    println("         - Quantity: ${item.quantity}")
                    println("         - UnitPrice: ${item.unitPrice}")
                    println("         - Subtotal: ${item.subtotal}")
                    println("         - Válido: ${item.isValid()}")
                }
            }

            println("✅ Verificación de estructura completada")
        } catch (e: Exception) {
            println("❌ Error verificando estructura: ${e.message}")
        }
    }
}
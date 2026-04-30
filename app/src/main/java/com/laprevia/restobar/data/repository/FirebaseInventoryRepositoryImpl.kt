package com.laprevia.restobar.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.laprevia.restobar.data.model.Inventory
import com.laprevia.restobar.domain.repository.FirebaseInventoryRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.laprevia.restobar.di.InventoryReference

@Singleton
class FirebaseInventoryRepositoryImpl @Inject constructor(
    @InventoryReference
    private val inventoryRef: DatabaseReference
) : FirebaseInventoryRepository {

    // ==================== MÉTODOS DE LA INTERFACE InventoryRepository ====================

    override fun getInventory(): Flow<List<Inventory>> = callbackFlow {
        println("🔥 FirebaseInventory: Suscribiéndose a todos los items de inventario")

        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val inventory = snapshot.children.mapNotNull { it.toInventory() }
                println("✅ FirebaseInventory: ${inventory.size} items cargados")

                println("📋 DETALLE COMPLETO DEL INVENTARIO:")
                if (inventory.isEmpty()) {
                    println("   ⚠️ NO HAY DATOS en la colección 'inventory'")
                } else {
                    inventory.forEachIndexed { index, item ->
                        println("   ${index + 1}. ID: '${item.productId}'")
                        println("      Nombre: '${item.productName}'")
                        println("      Stock: ${item.currentStock} ${item.unitOfMeasure}")
                        println("      Mínimo: ${item.minimumStock}")
                        println("      Categoría: '${item.category ?: "N/A"}'")
                    }
                }

                trySend(inventory)
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ FirebaseInventory: Error en getInventory: ${error.message}")
                close(error.toException())
            }
        }

        inventoryRef.addValueEventListener(eventListener)
        awaitClose {
            println("🔴 FirebaseInventory: Cerrando listener de inventario")
            inventoryRef.removeEventListener(eventListener)
        }
    }

    override fun getLowStockItems(): Flow<List<Inventory>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val inventory = snapshot.children.mapNotNull { it.toInventory() }
                    .filter { it.currentStock <= it.minimumStock }
                println("⚠️ FirebaseInventory: ${inventory.size} items con stock bajo")
                trySend(inventory)
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ FirebaseInventory: Error en getLowStockItems: ${error.message}")
                close(error.toException())
            }
        }

        inventoryRef.addValueEventListener(eventListener)
        awaitClose { inventoryRef.removeEventListener(eventListener) }
    }

    override suspend fun updateStock(productId: String, newQuantity: Double) {
        try {
            println("🔄 FirebaseInventory: Actualizando stock de $productId a $newQuantity")

            val updates = mapOf(
                "currentStock" to newQuantity
            )
            inventoryRef.child(productId).updateChildren(updates).await()

            println("✅ FirebaseInventory: Stock actualizado exitosamente")
        } catch (e: Exception) {
            println("❌ FirebaseInventory: Error actualizando stock: ${e.message}")
            throw e
        }
    }

    // ✅ MÉTODO: addInventoryItem
    override suspend fun addInventoryItem(item: Inventory) {
        try {
            println("📝 FirebaseInventory: Agregando item al inventario - ${item.productName}")

            val itemData = mapOf(
                "productId" to item.productId,
                "productName" to item.productName,
                "currentStock" to item.currentStock,
                "unitOfMeasure" to item.unitOfMeasure,
                "minimumStock" to item.minimumStock,
                "category" to (item.category ?: ""),
                "createdAt" to System.currentTimeMillis()
            )

            inventoryRef.child(item.productId).setValue(itemData).await()
            println("✅ FirebaseInventory: Item agregado exitosamente - ${item.productName}")

        } catch (e: Exception) {
            println("❌ FirebaseInventory: Error agregando item: ${e.message}")
            throw e
        }
    }

    // ✅ MÉTODO: deleteInventoryItem
    override suspend fun deleteInventoryItem(productId: String) {
        try {
            println("🗑️ FirebaseInventory: Eliminando item del inventario: $productId")

            val snapshot = inventoryRef.child(productId).get().await()
            if (snapshot.exists()) {
                inventoryRef.child(productId).removeValue().await()
                println("✅ FirebaseInventory: Item $productId eliminado exitosamente")
            } else {
                println("⚠️ FirebaseInventory: Item $productId no existe")
            }
        } catch (e: Exception) {
            println("❌ FirebaseInventory: Error eliminando item $productId: ${e.message}")
            throw e
        }
    }

    // ✅ NUEVO MÉTODO: getInventoryItemById (EL QUE FALTABA)
    override suspend fun getInventoryItemById(productId: String): Inventory? {
        return try {
            println("🔍 FirebaseInventory: Buscando item por ID: $productId")
            val snapshot = inventoryRef.child(productId).get().await()
            val inventory = snapshot.toInventory()

            if (inventory != null) {
                println("✅ FirebaseInventory: Item encontrado - ${inventory.productName}")
            } else {
                println("❌ FirebaseInventory: Item no encontrado - $productId")
            }

            inventory
        } catch (e: Exception) {
            println("❌ FirebaseInventory: Error obteniendo item $productId: ${e.message}")
            null
        }
    }

    // ✅ MÉTODO: updateInventoryFields
    override suspend fun updateInventoryFields(productId: String, updates: Map<String, Any>) {
        try {
            println("🔄 FirebaseInventory: Actualizando campos de $productId con $updates")

            val snapshot = inventoryRef.child(productId).get().await()
            if (!snapshot.exists()) {
                println("➕ FirebaseInventory: Creando nuevo producto $productId")
                val fullData = mutableMapOf<String, Any>()
                fullData.putAll(updates)
                fullData["productId"] = productId
                inventoryRef.child(productId).setValue(fullData).await()
            } else {
                inventoryRef.child(productId).updateChildren(updates).await()
            }

            println("✅ FirebaseInventory: Campos de $productId actualizados exitosamente")
        } catch (e: Exception) {
            println("❌ FirebaseInventory: Error actualizando campos de $productId: ${e.message}")
            throw e
        }
    }

    // ✅ MÉTODO: deleteProduct (alias)
    override suspend fun deleteProduct(productId: String) {
        deleteInventoryItem(productId)
    }

    override fun getInventoryByCategory(category: String): Flow<List<Inventory>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val inventory = snapshot.children.mapNotNull { it.toInventory() }
                    .filter { it.category == category }
                println("🏷️ FirebaseInventory: ${inventory.size} items en categoría $category")
                trySend(inventory)
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ FirebaseInventory: Error en getInventoryByCategory: ${error.message}")
                close(error.toException())
            }
        }

        inventoryRef.orderByChild("category").equalTo(category)
            .addValueEventListener(eventListener)

        awaitClose { inventoryRef.removeEventListener(eventListener) }
    }

    // ==================== MÉTODOS ESPECÍFICOS DE FIREBASE ====================

    override fun listenToInventoryChanges(): Flow<Inventory> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    child.toInventory()?.let { inventory ->
                        println("📡 FirebaseInventory: Cambio detectado en ${inventory.productName}")
                        trySend(inventory)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ FirebaseInventory: Error en listenToInventoryChanges: ${error.message}")
                close(error.toException())
            }
        }

        inventoryRef.addValueEventListener(eventListener)
        awaitClose { inventoryRef.removeEventListener(eventListener) }
    }

    override fun getLowStockAlerts(): Flow<List<Inventory>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lowStockItems = snapshot.children.mapNotNull { it.toInventory() }
                    .filter {
                        it.currentStock <= it.minimumStock && it.currentStock > 0
                    }

                if (lowStockItems.isNotEmpty()) {
                    println("🚨 FirebaseInventory: ALERTA - ${lowStockItems.size} items con stock crítico")
                    lowStockItems.forEach { item ->
                        println("   - ${item.productName}: ${item.currentStock} ${item.unitOfMeasure} (mínimo: ${item.minimumStock})")
                    }
                }

                trySend(lowStockItems)
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ FirebaseInventory: Error en getLowStockAlerts: ${error.message}")
                close(error.toException())
            }
        }

        inventoryRef.addValueEventListener(eventListener)
        awaitClose { inventoryRef.removeEventListener(eventListener) }
    }

    override suspend fun initializeDefaultInventory() {
        println("ℹ️ FirebaseInventory: El inventario se sincroniza desde los productos con trackInventory = true")

        try {
            val snapshot = inventoryRef.get().await()
            val currentCount = snapshot.children.count()
            println("📊 FirebaseInventory: Actualmente hay $currentCount items en inventario")

            if (currentCount == 0) {
                println("💡 FirebaseInventory: Usa el método initializeSampleData del ViewModel para crear datos")
            }
        } catch (e: Exception) {
            println("⚠️ FirebaseInventory: Error verificando estado: ${e.message}")
        }
    }

    override suspend fun getCurrentStock(productId: String): Double {
        return try {
            println("🔍 FirebaseInventory: Buscando stock para producto: $productId")
            val snapshot = inventoryRef.child(productId).child("currentStock").get().await()
            val stock = snapshot.getValue(Double::class.java) ?: 0.0
            println("✅ FirebaseInventory: Stock encontrado: $stock")
            stock
        } catch (e: Exception) {
            println("❌ FirebaseInventory: Error obteniendo stock de $productId: ${e.message}")
            0.0
        }
    }

    // ==================== MÉTODOS UTILITARIOS ====================

    private fun DataSnapshot.toInventory(): Inventory? {
        return try {
            val productId = key ?: return null
            val productName = child("productName").getValue(String::class.java) ?: ""
            val currentStock = child("currentStock").getValue(Double::class.java) ?: 0.0
            val unitOfMeasure = child("unitOfMeasure").getValue(String::class.java) ?: "unidades"
            val minimumStock = child("minimumStock").getValue(Double::class.java) ?: 0.0
            val category = child("category").getValue(String::class.java)

            if (productName.isBlank()) {
                println("⚠️ FirebaseInventory: Producto con ID '$productId' tiene nombre vacío")
            }

            Inventory(
                productId = productId,
                productName = productName,
                currentStock = currentStock,
                unitOfMeasure = unitOfMeasure,
                minimumStock = minimumStock,
                category = category
            )
        } catch (e: Exception) {
            println("❌ FirebaseInventory: Error convirtiendo DataSnapshot: ${e.message}")
            null
        }
    }

    private fun Inventory.toFirebaseMap(): Map<String, Any?> {
        return mapOf(
            "productId" to productId,
            "productName" to productName,
            "currentStock" to currentStock,
            "unitOfMeasure" to unitOfMeasure,
            "minimumStock" to minimumStock,
            "category" to category
        )
    }

    // ==================== MÉTODOS ADICIONALES UTILES ====================

    suspend fun adjustStock(productId: String, quantity: Double) {
        try {
            val currentStock = getCurrentStock(productId)
            val newStock = currentStock + quantity

            if (newStock < 0) {
                throw IllegalArgumentException("Stock no puede ser negativo")
            }

            updateStock(productId, newStock)
            println("📊 FirebaseInventory: Stock de $productId ajustado de $currentStock a $newStock")

        } catch (e: Exception) {
            println("❌ FirebaseInventory: Error ajustando stock: ${e.message}")
            throw e
        }
    }

    suspend fun hasSufficientStock(productId: String, requiredQuantity: Double): Boolean {
        return try {
            val currentStock = getCurrentStock(productId)
            currentStock >= requiredQuantity
        } catch (e: Exception) {
            println("❌ FirebaseInventory: Error verificando stock: ${e.message}")
            false
        }
    }

    suspend fun getInventoryStats(): Map<String, Any> {
        return try {
            val snapshot = inventoryRef.get().await()
            val inventory = snapshot.children.mapNotNull { it.toInventory() }

            val totalItems = inventory.size
            val lowStockCount = inventory.count { it.currentStock <= it.minimumStock }
            val outOfStockCount = inventory.count { it.currentStock <= 0 }
            val totalValue = inventory.sumOf { it.currentStock }

            mapOf(
                "totalItems" to totalItems,
                "lowStockCount" to lowStockCount,
                "outOfStockCount" to outOfStockCount,
                "totalValue" to totalValue
            )
        } catch (e: Exception) {
            println("❌ FirebaseInventory: Error obteniendo estadísticas: ${e.message}")
            emptyMap()
        }
    }

    suspend fun clearAllInventory() {
        try {
            println("🗑️ FirebaseInventory: LIMPIANDO TODO EL INVENTARIO...")
            val snapshot = inventoryRef.get().await()
            snapshot.children.forEach { child ->
                child.ref.removeValue().await()
            }
            println("✅ FirebaseInventory: Inventario limpiado exitosamente")
        } catch (e: Exception) {
            println("❌ FirebaseInventory: Error limpiando inventario: ${e.message}")
            throw e
        }
    }
}
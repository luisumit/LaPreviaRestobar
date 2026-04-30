package com.laprevia.restobar.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.repository.FirebaseProductRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.laprevia.restobar.di.ProductsReference

@Singleton
class FirebaseProductRepositoryImpl @Inject constructor(
    @ProductsReference
    private val productsRef: DatabaseReference
) : FirebaseProductRepository {

    // ==================== MÉTODO getProductById ====================

    override suspend fun getProductById(id: String): Product {
        return try {
            println("🔍 FirebaseProducts: Buscando producto por ID: $id")

            val snapshot = productsRef.child(id).get().await()

            if (snapshot.exists()) {
                val product = snapshot.toProduct()
                println("✅ FirebaseProducts: Producto encontrado - ${product.name} (ID: ${product.id})")
                println("   - Categoría: ${product.category}")
                println("   - Precio: ${product.salePrice}")
                println("   - TrackInventory: ${product.trackInventory}")
                product
            } else {
                println("❌ FirebaseProducts: Producto no encontrado - ID: $id")
                createDefaultProduct(id)
            }
        } catch (e: Exception) {
            println("❌ FirebaseProducts: Error obteniendo producto $id - ${e.message}")
            createDefaultProduct(id)
        }
    }

    private fun createDefaultProduct(productId: String): Product {
        return Product(
            id = productId,
            name = "Producto no disponible",
            description = "",
            category = "General",
            salePrice = null,
            costPrice = null,
            trackInventory = false,
            stock = 0.0,
            minStock = 0.0,
            imageUrl = null,
            isActive = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    // ==================== MÉTODO toProduct ====================

    private fun DataSnapshot.toProduct(): Product {
        return try {
            val id = key ?: "unknown_${System.currentTimeMillis()}"

            Product(
                id = id,
                name = child("name").getValue(String::class.java) ?: "Sin nombre",
                description = child("description").getValue(String::class.java) ?: "",
                category = child("category").getValue(String::class.java) ?: "General",
                salePrice = child("salePrice").getValue(Double::class.java),
                costPrice = child("costPrice").getValue(Double::class.java),
                trackInventory = child("trackInventory").getValue(Boolean::class.java) ?: false,
                stock = child("stock").getValue(Double::class.java) ?: 0.0,
                minStock = child("minStock").getValue(Double::class.java) ?: 0.0,
                imageUrl = child("imageUrl").getValue(String::class.java),
                isActive = child("isActive").getValue(Boolean::class.java) ?: true,
                createdAt = child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis(),
                updatedAt = child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            println("❌ FirebaseProducts: Error convirtiendo DataSnapshot: ${e.message}")
            Product(
                id = "error_${System.currentTimeMillis()}",
                name = "Error al cargar",
                description = "",
                category = "General",
                salePrice = null,
                costPrice = null,
                trackInventory = false,
                stock = 0.0,
                minStock = 0.0,
                imageUrl = null,
                isActive = false,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        }
    }

    // ==================== MÉTODOS DE ProductRepository ====================

    override fun getAllProducts(): Flow<List<Product>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = snapshot.children.map { it.toProduct() }
                println("🔥 FirebaseProducts: ${products.size} productos cargados")
                trySend(products)
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ FirebaseProducts: Error en getAllProducts: ${error.message}")
                close(error.toException())
            }
        }
        productsRef.addValueEventListener(eventListener)
        awaitClose { productsRef.removeEventListener(eventListener) }
    }

    override fun getActiveProducts(): Flow<List<Product>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = snapshot.children.map { it.toProduct() }
                    .filter { it.isActive }
                println("✅ FirebaseProducts: ${products.size} productos activos")
                trySend(products)
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ FirebaseProducts: Error en getActiveProducts: ${error.message}")
                close(error.toException())
            }
        }
        productsRef.addValueEventListener(eventListener)
        awaitClose { productsRef.removeEventListener(eventListener) }
    }

    override fun getSellableProducts(): Flow<List<Product>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = snapshot.children.map { it.toProduct() }
                    .filter { it.isActive }
                println("💰 FirebaseProducts: ${products.size} productos activos")
                trySend(products)
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ FirebaseProducts: Error en getSellableProducts: ${error.message}")
                close(error.toException())
            }
        }
        productsRef.addValueEventListener(eventListener)
        awaitClose { productsRef.removeEventListener(eventListener) }
    }

    override fun getCategories(): Flow<List<String>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categories = snapshot.children.map { it.toProduct() }
                    .map { it.category }
                    .distinct()
                    .sorted()
                println("🏷️ FirebaseProducts: ${categories.size} categorías encontradas")
                trySend(categories)
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ FirebaseProducts: Error en getCategories: ${error.message}")
                close(error.toException())
            }
        }
        productsRef.addValueEventListener(eventListener)
        awaitClose { productsRef.removeEventListener(eventListener) }
    }

    override fun getProducts(): Flow<List<Product>> = getAllProducts()

    // ==================== MÉTODOS CRUD ====================

    override suspend fun getProductByName(name: String): Product? {
        return try {
            println("🔍 FirebaseProducts: Buscando producto por nombre: $name")
            val snapshot = productsRef.orderByChild("name").equalTo(name).get().await()
            val product = snapshot.children.firstOrNull()?.toProduct()
            if (product != null) {
                println("✅ FirebaseProducts: Producto encontrado: ${product.name}")
            } else {
                println("❌ FirebaseProducts: Producto no encontrado: $name")
            }
            product
        } catch (e: Exception) {
            println("❌ FirebaseProducts: Error buscando producto por nombre $name: ${e.message}")
            null
        }
    }

    override suspend fun createProduct(product: Product) {
        try {
            println("🆕 FirebaseProducts: Creando producto: ${product.name}")
            val productMap = toFirebaseMap(product)
            productsRef.child(product.id).setValue(productMap).await()
            println("✅ FirebaseProducts: Producto creado exitosamente: ${product.name}")
        } catch (e: Exception) {
            println("❌ FirebaseProducts: Error creando producto: ${e.message}")
            throw e
        }
    }

    // ✅ MÉTODO AGREGADO - updateProduct
    override suspend fun updateProduct(product: Product) {
        try {
            println("🔄 FirebaseProducts: Actualizando producto: ${product.name}")
            val productMap = toFirebaseMap(product)
            productsRef.child(product.id).updateChildren(productMap).await()
            println("✅ FirebaseProducts: Producto actualizado exitosamente: ${product.name}")
        } catch (e: Exception) {
            println("❌ FirebaseProducts: Error actualizando producto: ${e.message}")
            throw e
        }
    }

    override suspend fun updateProductStatus(id: String, isActive: Boolean) {
        try {
            println("⚡ FirebaseProducts: Actualizando estado de producto $id a $isActive")
            val updates = mapOf(
                "isActive" to isActive,
                "updatedAt" to System.currentTimeMillis()
            )
            productsRef.child(id).updateChildren(updates).await()
            println("✅ FirebaseProducts: Estado actualizado exitosamente")
        } catch (e: Exception) {
            println("❌ FirebaseProducts: Error actualizando estado: ${e.message}")
            throw e
        }
    }

    override suspend fun deleteProduct(id: String) {
        try {
            println("🗑️ FirebaseProducts: Eliminando producto: $id")
            productsRef.child(id).removeValue().await()
            println("✅ FirebaseProducts: Producto eliminado exitosamente")
        } catch (e: Exception) {
            println("❌ FirebaseProducts: Error eliminando producto: ${e.message}")
            throw e
        }
    }


    // ==================== MÉTODOS ESPECÍFICOS DE FIREBASE ====================

    override fun listenToProductChanges(): Flow<Product> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    val product = child.toProduct()
                    println("📡 FirebaseProducts: Cambio detectado en ${product.name}")
                    trySend(product)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ FirebaseProducts: Error en listenToProductChanges: ${error.message}")
                close(error.toException())
            }
        }
        productsRef.addValueEventListener(eventListener)
        awaitClose { productsRef.removeEventListener(eventListener) }
    }

    override fun getProductsRealTime(): Flow<List<Product>> = getAllProducts()

    // ==================== MÉTODOS DE INVENTARIO ====================

    override fun getProductsWithInventory(): Flow<List<Product>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val products = snapshot.children.map { it.toProduct() }
                    .filter { it.trackInventory }
                println("📦 FirebaseProducts: ${products.size} productos con control de inventario")
                trySend(products)
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ FirebaseProducts: Error en getProductsWithInventory: ${error.message}")
                close(error.toException())
            }
        }
        productsRef.addValueEventListener(eventListener)
        awaitClose { productsRef.removeEventListener(eventListener) }
    }

    override suspend fun updateProductStock(productId: String, newQuantity: Double) {
        try {
            println("📊 FirebaseProducts: Actualizando stock de $productId a $newQuantity")
            val updates = mapOf(
                "stock" to newQuantity,
                "updatedAt" to System.currentTimeMillis()
            )
            productsRef.child(productId).updateChildren(updates).await()
            println("✅ FirebaseProducts: Stock actualizado exitosamente")
        } catch (e: Exception) {
            println("❌ FirebaseProducts: Error actualizando stock: ${e.message}")
            throw e
        }
    }

    // ==================== MÉTODOS ADICIONALES ====================

    override suspend fun searchProducts(query: String): List<Product> {
        return try {
            println("🔍 FirebaseProducts: Buscando productos con query: $query")
            val snapshot = productsRef.get().await()
            val products = snapshot.children.map { it.toProduct() }
                .filter { product ->
                    product.name.contains(query, ignoreCase = true) ||
                            product.description.contains(query, ignoreCase = true)
                }
            println("✅ FirebaseProducts: ${products.size} productos encontrados para '$query'")
            products
        } catch (e: Exception) {
            println("❌ FirebaseProducts: Error buscando productos: ${e.message}")
            emptyList()
        }
    }

    override suspend fun getProductsByCategory(category: String): List<Product> {
        return try {
            println("🏷️ FirebaseProducts: Buscando productos en categoría: $category")
            val snapshot = productsRef.orderByChild("category").equalTo(category).get().await()
            val products = snapshot.children.map { it.toProduct() }
            println("✅ FirebaseProducts: ${products.size} productos en categoría '$category'")
            products
        } catch (e: Exception) {
            println("❌ FirebaseProducts: Error obteniendo productos por categoría: ${e.message}")
            emptyList()
        }
    }

    override suspend fun productExists(productId: String): Boolean {
        return try {
            val snapshot = productsRef.child(productId).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            println("❌ FirebaseProducts: Error verificando existencia de producto: ${e.message}")
            false
        }
    }

    override suspend fun getProductStats(): Map<String, Any> {
        return try {
            val snapshot = productsRef.get().await()
            val products = snapshot.children.map { it.toProduct() }

            mapOf(
                "totalProducts" to products.size,
                "activeProducts" to products.count { it.isActive },
                "trackInventoryProducts" to products.count { it.trackInventory },
                "categoriesCount" to products.map { it.category }.distinct().count()
            )
        } catch (e: Exception) {
            println("❌ FirebaseProducts: Error obteniendo estadísticas: ${e.message}")
            emptyMap()
        }
    }

    // ==================== MÉTODO toFirebaseMap ====================

    private fun toFirebaseMap(product: Product): Map<String, Any?> = mapOf(
        "id" to product.id,
        "name" to product.name,
        "description" to product.description,
        "category" to product.category,
        "salePrice" to product.salePrice,
        "costPrice" to product.costPrice,
        "trackInventory" to product.trackInventory,
        "stock" to product.stock,
        "minStock" to product.minStock,
        "imageUrl" to product.imageUrl,
        "isActive" to product.isActive,
        "createdAt" to product.createdAt,
        "updatedAt" to product.updatedAt
    )
}
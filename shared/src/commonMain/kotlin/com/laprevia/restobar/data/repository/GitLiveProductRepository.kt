package com.laprevia.restobar.data.repository

import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.domain.repository.FirebaseProductRepository
import com.laprevia.restobar.platform.currentTimeMillis
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.DataSnapshot
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repositorio de productos MULTIPLATAFORMA (GitLive Firebase): Android, Desktop/JVM y Web JS.
 * Conserva EXACTAMENTE el mismo formato de datos que la implementacion nativa
 * (mismos nombres de campo y tipos) — la base en produccion ya tiene datos.
 *
 * La lectura usa el valor crudo del snapshot con conversion numerica tolerante
 * (Firebase entrega los enteros como Long), igual de permisiva que el SDK nativo.
 */
class GitLiveProductRepository : FirebaseProductRepository {

    private val productsRef: DatabaseReference get() = Firebase.database.reference("products")

    // ==================== LISTADOS / TIEMPO REAL ====================

    override fun getAllProducts(): Flow<List<Product>> =
        productsRef.valueEvents.map { snapshot -> snapshot.children.map { it.toProduct() } }

    override fun getProducts(): Flow<List<Product>> = getAllProducts()

    override fun getProductsRealTime(): Flow<List<Product>> = getAllProducts()

    override fun getActiveProducts(): Flow<List<Product>> =
        getAllProducts().map { products -> products.filter { it.isActive } }

    override fun getSellableProducts(): Flow<List<Product>> =
        getAllProducts().map { products -> products.filter { it.isActive } }

    override fun getProductsWithInventory(): Flow<List<Product>> =
        getAllProducts().map { products -> products.filter { it.trackInventory } }

    override fun getCategories(): Flow<List<String>> =
        getAllProducts().map { products -> products.map { it.category }.distinct().sorted() }

    override fun listenToProductChanges(): Flow<Product> =
        getAllProducts().map { it.lastOrNull() ?: Product() }

    // ==================== BUSQUEDA ====================

    override suspend fun getProductById(id: String): Product = runCatching {
        val snapshot = productsRef.child(id).valueEvents.first()
        if (snapshot.exists) snapshot.toProduct() else defaultProduct(id)
    }.getOrElse { defaultProduct(id) }

    override suspend fun getProductByName(name: String): Product? = runCatching {
        productsRef.orderByChild("name").equalTo(name).valueEvents.first()
            .children.firstOrNull()?.toProduct()
    }.getOrNull()

    override suspend fun searchProducts(query: String): List<Product> = runCatching {
        productsRef.valueEvents.first().children.map { it.toProduct() }
            .filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.description.contains(query, ignoreCase = true)
            }
    }.getOrDefault(emptyList())

    override suspend fun getProductsByCategory(category: String): List<Product> = runCatching {
        productsRef.orderByChild("category").equalTo(category).valueEvents.first()
            .children.map { it.toProduct() }
    }.getOrDefault(emptyList())

    override suspend fun productExists(productId: String): Boolean = runCatching {
        productsRef.child(productId).valueEvents.first().exists
    }.getOrDefault(false)

    override suspend fun getProductStats(): Map<String, Any> = runCatching {
        val products = productsRef.valueEvents.first().children.map { it.toProduct() }
        mapOf<String, Any>(
            "totalProducts" to products.size,
            "activeProducts" to products.count { it.isActive },
            "trackInventoryProducts" to products.count { it.trackInventory },
            "categoriesCount" to products.map { it.category }.distinct().count()
        )
    }.getOrDefault(emptyMap())

    // ==================== CRUD ====================

    override suspend fun createProduct(product: Product) {
        productsRef.child(product.id).updateChildren(product.toFirebaseMap())
    }

    override suspend fun updateProduct(product: Product) {
        productsRef.child(product.id).updateChildren(product.toFirebaseMap())
    }

    override suspend fun updateProductStatus(id: String, isActive: Boolean) {
        productsRef.child(id).updateChildren(
            mapOf("isActive" to isActive, "updatedAt" to currentTimeMillis())
        )
    }

    override suspend fun deleteProduct(id: String) {
        productsRef.child(id).removeValue()
    }

    // ==================== STOCK ====================

    override suspend fun getProductStock(productId: String): Double = runCatching {
        (productsRef.child(productId).child("stock").valueEvents.first().value as? Number)
            ?.toDouble() ?: 0.0
    }.getOrDefault(0.0)

    override suspend fun updateProductStock(productId: String, newStock: Double) {
        productsRef.child(productId).updateChildren(
            mapOf("stock" to newStock, "updatedAt" to currentTimeMillis())
        )
    }

    // ==================== CONVERSION (mismo wire format que el SDK nativo) ====================

    private fun defaultProduct(productId: String): Product = Product(
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
        isActive = false
    )

    private fun DataSnapshot.toProduct(): Product = runCatching {
        Product(
            id = key ?: "unknown_${currentTimeMillis()}",
            name = child("name").value as? String ?: "Sin nombre",
            description = child("description").value as? String ?: "",
            category = child("category").value as? String ?: "General",
            salePrice = (child("salePrice").value as? Number)?.toDouble(),
            costPrice = (child("costPrice").value as? Number)?.toDouble(),
            trackInventory = child("trackInventory").value as? Boolean ?: false,
            stock = (child("stock").value as? Number)?.toDouble() ?: 0.0,
            minStock = (child("minStock").value as? Number)?.toDouble() ?: 0.0,
            imageUrl = child("imageUrl").value as? String,
            isActive = child("isActive").value as? Boolean ?: true,
            createdAt = (child("createdAt").value as? Number)?.toLong() ?: currentTimeMillis(),
            updatedAt = (child("updatedAt").value as? Number)?.toLong() ?: currentTimeMillis()
        )
    }.getOrElse {
        Product(
            id = "error_${currentTimeMillis()}",
            name = "Error al cargar",
            description = "",
            category = "General",
            isActive = false
        )
    }

    private fun Product.toFirebaseMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "description" to description,
        "category" to category,
        "salePrice" to salePrice,
        "costPrice" to costPrice,
        "trackInventory" to trackInventory,
        "stock" to stock,
        "minStock" to minStock,
        "imageUrl" to imageUrl,
        "isActive" to isActive,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )
}

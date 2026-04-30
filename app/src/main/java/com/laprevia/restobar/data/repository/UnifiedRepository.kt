package com.laprevia.restobar.data.repository

import com.laprevia.restobar.data.local.dao.*
import com.laprevia.restobar.data.local.entity.*
import com.laprevia.restobar.data.mapper.*
import com.laprevia.restobar.data.model.*
import com.laprevia.restobar.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnifiedOrderRepository @Inject constructor(
    private val localDao: OrderDao,
    private val remoteRepo: FirebaseOrderRepository
) {

    suspend fun createOrder(order: Order): String {
        val entity = order.toEntity()

        return try {
            // Intentar guardar en Firebase primero
            remoteRepo.createOrder(order)
            // Si funciona, guardar local como SYNCED
            localDao.insert(entity.copy(syncStatus = "SYNCED"))
            println("✅ Order created and synced: ${order.id}")
            order.id
        } catch (e: Exception) {
            // Sin internet, guardar local como PENDING
            localDao.insert(entity.copy(syncStatus = "PENDING"))
            println("💾 Order saved locally (offline): ${order.id}")
            order.id
        }
    }

    fun getAllOrders(): Flow<List<Order>> = flow {
        // Emitir locales inmediatamente
        val localOrders = localDao.getAll().map { it.toDomain() }
        emit(localOrders)

        // Luego emitir remotos cuando lleguen
        remoteRepo.getOrders().collect { remoteOrders ->
            // Actualizar Room con datos remotos
            remoteOrders.forEach { order ->
                localDao.insert(order.toEntity().copy(syncStatus = "SYNCED"))
            }
            // Emitir combinación
            val combined = remoteOrders + localDao.getPending().map { it.toDomain() }
            emit(combined.distinctBy { it.id })
        }
    }
}

@Singleton
class UnifiedProductRepository @Inject constructor(
    private val localDao: ProductDao,
    private val remoteRepo: FirebaseProductRepository
) {

    suspend fun createProduct(product: Product): String {
        val entity = product.toEntity()

        return try {
            remoteRepo.createProduct(product)
            localDao.insert(entity.copy(syncStatus = "SYNCED"))
            product.id
        } catch (e: Exception) {
            localDao.insert(entity.copy(syncStatus = "PENDING"))
            product.id
        }
    }

    fun getAllProducts(): Flow<List<Product>> = flow {
        val localProducts = localDao.getAll().map { it.toDomain() }
        emit(localProducts)

        remoteRepo.getAllProducts().collect { remoteProducts ->
            remoteProducts.forEach { product ->
                localDao.insert(product.toEntity().copy(syncStatus = "SYNCED"))
            }
            val combined = remoteProducts + localDao.getPending().map { it.toDomain() }
            emit(combined.distinctBy { it.id })
        }
    }

    suspend fun updateProductStock(productId: String, newStock: Double) {
        val localProduct = localDao.getById(productId)
        val newVersion = System.currentTimeMillis()

        try {
            remoteRepo.updateProductStock(productId, newStock)
            localDao.insert(
                localProduct?.copy(
                    stock = newStock,
                    syncStatus = "SYNCED",
                    version = newVersion,
                    lastModified = newVersion
                ) ?: ProductEntity(
                    id = productId, name = "", description = "", category = "",
                    salePrice = null, costPrice = null, trackInventory = false,
                    stock = newStock, minStock = 0.0, isActive = true,
                    syncStatus = "SYNCED", version = newVersion, lastModified = newVersion
                )
            )
        } catch (e: Exception) {
            localDao.insert(
                localProduct?.copy(
                    stock = newStock,
                    syncStatus = "PENDING",
                    version = newVersion,
                    lastModified = newVersion
                ) ?: ProductEntity(
                    id = productId, name = "", description = "", category = "",
                    salePrice = null, costPrice = null, trackInventory = false,
                    stock = newStock, minStock = 0.0, isActive = true,
                    syncStatus = "PENDING", version = newVersion, lastModified = newVersion
                )
            )
        }
    }
}

@Singleton
class UnifiedTableRepository @Inject constructor(
    private val localDao: TableDao,
    private val remoteRepo: FirebaseTableRepository
) {

    suspend fun updateTable(table: Table) {
        val entity = table.toEntity()

        try {
            remoteRepo.updateTable(table)
            localDao.insert(entity.copy(syncStatus = "SYNCED"))
        } catch (e: Exception) {
            localDao.insert(entity.copy(syncStatus = "PENDING"))
        }
    }

    fun getAllTables(): Flow<List<Table>> = flow {
        val localTables = localDao.getAll().map { it.toDomain() }
        emit(localTables)

        remoteRepo.getTables().collect { remoteTables ->
            remoteTables.forEach { table ->
                localDao.insert(table.toEntity().copy(syncStatus = "SYNCED"))
            }
            val combined = remoteTables + localDao.getPending().map { it.toDomain() }
            emit(combined.distinctBy { it.id })
        }
    }
}

@Singleton
class UnifiedInventoryRepository @Inject constructor(
    private val localDao: InventoryDao,
    private val remoteRepo: FirebaseInventoryRepository
) {

    suspend fun updateInventory(item: Inventory) {
        val entity = item.toEntity()

        try {
            remoteRepo.updateInventoryFields(item.productId, mapOf(
                "currentStock" to item.currentStock,
                "productName" to item.productName,
                "category" to (item.category ?: ""),
                "version" to entity.version,
                "lastModified" to entity.lastModified
            ))
            localDao.insert(entity.copy(syncStatus = "SYNCED"))
        } catch (e: Exception) {
            localDao.insert(entity.copy(syncStatus = "PENDING"))
        }
    }

    fun getAllInventory(): Flow<List<Inventory>> = flow {
        val localInventory = localDao.getAll().map { it.toDomain() }
        emit(localInventory)

        remoteRepo.getInventory().collect { remoteInventory ->
            remoteInventory.forEach { item ->
                localDao.insert(item.toEntity().copy(syncStatus = "SYNCED"))
            }
            val combined = remoteInventory + localDao.getPending().map { it.toDomain() }
            emit(combined.distinctBy { it.productId })
        }
    }
}
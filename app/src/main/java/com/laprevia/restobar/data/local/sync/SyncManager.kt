package com.laprevia.restobar.data.local.sync

import com.laprevia.restobar.data.local.db.AppDatabase
import com.laprevia.restobar.data.mapper.*
import com.laprevia.restobar.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    private val db: AppDatabase,
    private val firebaseOrders: FirebaseOrderRepository,
    private val firebaseTables: FirebaseTableRepository,
    private val firebaseInventory: FirebaseInventoryRepository,
    private val firebaseProducts: FirebaseProductRepository
) {

    suspend fun syncOrders() = withContext(Dispatchers.IO) {
        try {
            val pending = db.orderDao().getPending()
            Timber.d("🔄 Sync Orders: %d pendientes", pending.size)

            pending.forEach { entity ->
                try {
                    firebaseOrders.createOrder(entity.toDomain())
                    db.orderDao().updateStatus(entity.id, "SYNCED")
                    Timber.d("✅ Order sync OK: %s", entity.id)
                } catch (e: Exception) {
                    Timber.e(e, "❌ Order sync FAIL: %s", entity.id)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error en syncOrders")
        }
    }

    suspend fun syncTables() = withContext(Dispatchers.IO) {
        try {
            val pending = db.tableDao().getPending()
            Timber.d("🔄 Sync Tables: %d pendientes", pending.size)

            pending.forEach { entity ->
                try {
                    firebaseTables.updateTable(entity.toDomain())
                    db.tableDao().updateStatus(entity.id, "SYNCED")
                    Timber.d("✅ Table sync OK: %s", entity.id)
                } catch (e: Exception) {
                    Timber.e(e, "❌ Table sync FAIL: %s", entity.id)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error en syncTables")
        }
    }

    suspend fun syncInventory() = withContext(Dispatchers.IO) {
        try {
            val pending = db.inventoryDao().getPending()
            Timber.d("🔄 Sync Inventory: %d pendientes", pending.size)

            pending.forEach { entity ->
                try {
                    firebaseInventory.updateInventoryFields(
                        entity.productId,
                        mapOf(
                            "currentStock" to entity.currentStock,
                            "productName" to entity.productName,
                            "category" to (entity.category ?: ""),
                            "lastModified" to System.currentTimeMillis()
                        )
                    )
                    db.inventoryDao().updateStatus(entity.productId, "SYNCED")
                    Timber.d("✅ Inventory sync OK: %s", entity.productId)
                } catch (e: Exception) {
                    Timber.e(e, "❌ Inventory sync FAIL: %s", entity.productId)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error en syncInventory")
        }
    }

    suspend fun syncProducts() = withContext(Dispatchers.IO) {
        try {
            val pending = db.productDao().getPending()
            Timber.d("🔄 Sync Products: %d pendientes", pending.size)

            pending.forEach { entity ->
                try {
                    firebaseProducts.updateProduct(entity.toDomain())
                    db.productDao().updateStatus(entity.id, "SYNCED")
                    Timber.d("✅ Product sync OK: %s", entity.id)
                } catch (e: Exception) {
                    Timber.e(e, "❌ Product sync FAIL: %s", entity.id)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error en syncProducts")
        }
    }

    suspend fun downloadOrders() = withContext(Dispatchers.IO) {
        try {
            Timber.d("📥 Downloading orders from Firebase...")
            val firebaseOrdersList = firebaseOrders.getOrders().first()

            firebaseOrdersList.forEach { remoteOrder ->
                val localOrder = db.orderDao().getById(remoteOrder.id)
                if (localOrder == null || remoteOrder.updatedAt > localOrder.updatedAt) {
                    db.orderDao().insert(remoteOrder.toEntity().copy(syncStatus = "SYNCED"))
                    Timber.d("✅ Order updated: %s", remoteOrder.id)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error downloading orders")
        }
    }

    suspend fun downloadTables() = withContext(Dispatchers.IO) {
        try {
            Timber.d("📥 Downloading tables from Firebase...")
            val firebaseTablesList = firebaseTables.getTables().first()

            firebaseTablesList.forEach { remoteTable ->
                db.tableDao().insert(remoteTable.toEntity().copy(syncStatus = "SYNCED"))
                Timber.d("🔄 Table updated: %d", remoteTable.number)
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error downloading tables")
        }
    }

    suspend fun downloadInventory() = withContext(Dispatchers.IO) {
        try {
            Timber.d("📥 Downloading inventory from Firebase...")
            val firebaseInventoryList = firebaseInventory.getInventory().first()

            firebaseInventoryList.forEach { remoteItem ->
                val localItem = db.inventoryDao().getById(remoteItem.productId)
                if (localItem == null || remoteItem.currentStock != localItem.currentStock) {
                    db.inventoryDao().insert(remoteItem.toEntity().copy(syncStatus = "SYNCED"))
                    Timber.d("🔄 Inventory updated: %s", remoteItem.productName)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error downloading inventory")
        }
    }

    suspend fun downloadProducts() = withContext(Dispatchers.IO) {
        try {
            Timber.d("📥 Downloading products from Firebase...")
            val firebaseProductsList = firebaseProducts.getAllProducts().first()

            firebaseProductsList.forEach { remoteProduct ->
                val localProduct = db.productDao().getById(remoteProduct.id)
                if (localProduct == null || remoteProduct.updatedAt > localProduct.updatedAt) {
                    db.productDao().insert(remoteProduct.toEntity().copy(syncStatus = "SYNCED"))
                    Timber.d("🔄 Product updated: %s", remoteProduct.name)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error downloading products")
        }
    }

    suspend fun uploadAll() = withContext(Dispatchers.IO) {
        Timber.i("🚀 START UPLOAD TO FIREBASE")
        try {
            withTimeout(30000) {
                syncOrders()
                syncTables()
                syncInventory()
                syncProducts()
            }
            Timber.i("✅ UPLOAD COMPLETED")
        } catch (e: Exception) {
            Timber.e(e, "❌ Upload error")
        }
    }

    suspend fun downloadAll() = withContext(Dispatchers.IO) {
        Timber.i("🚀 START DOWNLOAD FROM FIREBASE")
        try {
            withTimeout(30000) {
                downloadOrders()
                downloadTables()
                downloadInventory()
                downloadProducts()
            }
            Timber.i("✅ DOWNLOAD COMPLETED")
        } catch (e: Exception) {
            Timber.e(e, "❌ Download error")
        }
    }

    suspend fun syncFull() = coroutineScope {
        Timber.i("🔄 FULL SYNC STARTED")
        try {
            withTimeout(60000) {
                uploadAll()
                downloadAll()
            }
            Timber.i("✅ FULL SYNC COMPLETED")
        } catch (e: Exception) {
            Timber.e(e, "❌ Full sync error")
        }
    }

    suspend fun syncLight() = withContext(Dispatchers.IO) {
        Timber.d("🔄 LIGHT SYNC")
        try {
            withTimeout(15000) {
                syncOrders()
            }
        } catch (e: Exception) {
            Timber.e(e, "⚠️ Light sync error")
        }
    }
}
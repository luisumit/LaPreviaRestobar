package com.laprevia.restobar.data.local.sync

import com.laprevia.restobar.data.local.db.AppDatabase
import com.laprevia.restobar.data.mapper.*
import com.laprevia.restobar.domain.repository.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
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

    // ================= SUBIR DATOS A FIREBASE =================

    suspend fun syncOrders() = withContext(Dispatchers.IO) {
        try {
            val pending = db.orderDao().getPending()
            println("🔄 Sync Orders: ${pending.size} pendientes")

            pending.forEach { entity ->
                try {
                    firebaseOrders.createOrder(entity.toDomain())
                    db.orderDao().updateStatus(entity.id, "SYNCED")
                    println("✅ Order sync OK: ${entity.id}")
                } catch (e: Exception) {
                    println("❌ Order sync FAIL: ${entity.id} -> ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("❌ Error en syncOrders: ${e.message}")
        }
    }

    suspend fun syncTables() = withContext(Dispatchers.IO) {
        try {
            val pending = db.tableDao().getPending()
            println("🔄 Sync Tables: ${pending.size} pendientes")

            pending.forEach { entity ->
                try {
                    firebaseTables.updateTable(entity.toDomain())
                    db.tableDao().updateStatus(entity.id, "SYNCED")
                    println("✅ Table sync OK: ${entity.id}")
                } catch (e: Exception) {
                    println("❌ Table sync FAIL: ${entity.id} -> ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("❌ Error en syncTables: ${e.message}")
        }
    }

    suspend fun syncInventory() = withContext(Dispatchers.IO) {
        try {
            val pending = db.inventoryDao().getPending()
            println("🔄 Sync Inventory: ${pending.size} pendientes")

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
                    println("✅ Inventory sync OK: ${entity.productId}")
                } catch (e: Exception) {
                    println("❌ Inventory sync FAIL: ${entity.productId} -> ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("❌ Error en syncInventory: ${e.message}")
        }
    }

    suspend fun syncProducts() = withContext(Dispatchers.IO) {
        try {
            val pending = db.productDao().getPending()
            println("🔄 Sync Products: ${pending.size} pendientes")

            pending.forEach { entity ->
                try {
                    firebaseProducts.updateProduct(entity.toDomain())
                    db.productDao().updateStatus(entity.id, "SYNCED")
                    println("✅ Product sync OK: ${entity.id}")
                } catch (e: Exception) {
                    println("❌ Product sync FAIL: ${entity.id} -> ${e.message}")
                }
            }
        } catch (e: Exception) {
            println("❌ Error en syncProducts: ${e.message}")
        }
    }

    // ================= DESCARGAR DATOS DE FIREBASE =================

    suspend fun downloadOrders() = withContext(Dispatchers.IO) {
        try {
            println("📥 Downloading orders from Firebase...")
            val firebaseOrdersList = firebaseOrders.getOrders().first()

            firebaseOrdersList.forEach { remoteOrder ->
                val localOrder = db.orderDao().getById(remoteOrder.id)

                if (localOrder == null) {
                    db.orderDao().insert(remoteOrder.toEntity().copy(syncStatus = "SYNCED"))
                    println("✅ New order saved: ${remoteOrder.id}")
                } else if (remoteOrder.createdAt > localOrder.createdAt) {
                    // Usar createdAt en lugar de updatedAt
                    db.orderDao().insert(remoteOrder.toEntity().copy(syncStatus = "SYNCED"))
                    println("🔄 Order updated: ${remoteOrder.id}")
                }
            }
        } catch (e: Exception) {
            println("❌ Error downloading orders: ${e.message}")
        }
    }

    suspend fun downloadTables() = withContext(Dispatchers.IO) {
        try {
            println("📥 Downloading tables from Firebase...")
            val firebaseTablesList = firebaseTables.getTables().first()

            firebaseTablesList.forEach { remoteTable ->
                val localTable = db.tableDao().getById(remoteTable.id)

                if (localTable == null) {
                    db.tableDao().insert(remoteTable.toEntity().copy(syncStatus = "SYNCED"))
                    println("✅ New table saved: ${remoteTable.number}")
                } else {
                    // Siempre actualizar si es diferente
                    db.tableDao().insert(remoteTable.toEntity().copy(syncStatus = "SYNCED"))
                    println("🔄 Table updated: ${remoteTable.number}")
                }
            }
        } catch (e: Exception) {
            println("❌ Error downloading tables: ${e.message}")
        }
    }

    suspend fun downloadInventory() = withContext(Dispatchers.IO) {
        try {
            println("📥 Downloading inventory from Firebase...")
            val firebaseInventoryList = firebaseInventory.getInventory().first()

            firebaseInventoryList.forEach { remoteItem ->
                val localItem = db.inventoryDao().getById(remoteItem.productId)

                if (localItem == null) {
                    db.inventoryDao().insert(remoteItem.toEntity().copy(syncStatus = "SYNCED"))
                    println("✅ New inventory item saved: ${remoteItem.productName}")
                } else if (remoteItem.currentStock != localItem.currentStock) {
                    db.inventoryDao().insert(remoteItem.toEntity().copy(syncStatus = "SYNCED"))
                    println("🔄 Inventory updated: ${remoteItem.productName}")
                }
            }
        } catch (e: Exception) {
            println("❌ Error downloading inventory: ${e.message}")
        }
    }

    suspend fun downloadProducts() = withContext(Dispatchers.IO) {
        try {
            println("📥 Downloading products from Firebase...")
            val firebaseProductsList = firebaseProducts.getAllProducts().first()

            firebaseProductsList.forEach { remoteProduct ->
                val localProduct = db.productDao().getById(remoteProduct.id)

                if (localProduct == null) {
                    db.productDao().insert(remoteProduct.toEntity().copy(syncStatus = "SYNCED"))
                    println("✅ New product saved: ${remoteProduct.name}")
                } else if (remoteProduct.updatedAt > localProduct.updatedAt) {
                    // updatedAt existe en Product
                    db.productDao().insert(remoteProduct.toEntity().copy(syncStatus = "SYNCED"))
                    println("🔄 Product updated: ${remoteProduct.name}")
                }
            }
        } catch (e: Exception) {
            println("❌ Error downloading products: ${e.message}")
        }
    }

    // ================= SYNC COMPLETO =================

    suspend fun uploadAll() = withContext(Dispatchers.IO) {
        println("🚀 START UPLOAD TO FIREBASE")

        try {
            withTimeout(30000) {
                syncOrders()
                syncTables()
                syncInventory()
                syncProducts()
            }
            println("✅ UPLOAD COMPLETED")
        } catch (e: TimeoutCancellationException) {
            println("⚠️ Upload timeout")
        } catch (e: Exception) {
            println("❌ Upload error: ${e.message}")
        }
    }

    suspend fun downloadAll() = withContext(Dispatchers.IO) {
        println("🚀 START DOWNLOAD FROM FIREBASE")

        try {
            withTimeout(30000) {
                downloadOrders()
                downloadTables()
                downloadInventory()
                downloadProducts()
            }
            println("✅ DOWNLOAD COMPLETED")
        } catch (e: TimeoutCancellationException) {
            println("⚠️ Download timeout")
        } catch (e: Exception) {
            println("❌ Download error: ${e.message}")
        }
    }

    suspend fun syncFull() = coroutineScope {
        println("🔄 FULL SYNC STARTED")

        try {
            withTimeout(60000) {
                uploadAll()
                downloadAll()
            }
            println("✅ FULL SYNC COMPLETED")
        } catch (e: TimeoutCancellationException) {
            println("⚠️ Full sync timeout")
        } catch (e: Exception) {
            println("❌ Full sync error: ${e.message}")
        }
    }

    suspend fun syncLight() = withContext(Dispatchers.IO) {
        println("🔄 LIGHT SYNC")
        try {
            withTimeout(15000) {
                syncOrders()
            }
        } catch (e: Exception) {
            println("⚠️ Light sync error: ${e.message}")
        }
    }

    suspend fun resolveConflicts() = withContext(Dispatchers.IO) {
        println("⚖️ Resolving conflicts...")
        try {
            val localOrders = db.orderDao().getAll()
            val remoteOrders = firebaseOrders.getOrders().first()

            remoteOrders.forEach { remote ->
                val local = localOrders.find { it.id == remote.id }
                if (local != null && local.createdAt != remote.createdAt) {
                    if (remote.createdAt > local.createdAt) {
                        println("📥 Remote wins for order ${remote.id}")
                        db.orderDao().insert(remote.toEntity().copy(syncStatus = "SYNCED"))
                    } else if (local.createdAt > remote.createdAt) {
                        println("📤 Local wins for order ${remote.id}")
                        firebaseOrders.updateOrder(local.toDomain())
                    }
                }
            }
            println("✅ Conflict resolution completed")
        } catch (e: Exception) {
            println("❌ Error resolving conflicts: ${e.message}")
        }
    }
}
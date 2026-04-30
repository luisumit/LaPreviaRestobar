package com.laprevia.restobar.core.sync
import com.laprevia.restobar.data.mapper.*
import com.laprevia.restobar.data.repository.*
import com.laprevia.restobar.domain.repository.*
import javax.inject.Inject

class SyncManager @Inject constructor(

    private val orderRepo: OrderRepositoryImpl,
    private val productRepo: ProductRepositoryImpl,
    private val inventoryRepo: InventoryRepositoryImpl,
    private val tableRepo: TableRepositoryImpl,

    private val firebaseOrder: FirebaseOrderRepository,
    private val firebaseProduct: FirebaseProductRepository,
    private val firebaseInventory: FirebaseInventoryRepository,
    private val firebaseTable: FirebaseTableRepository
) {

    suspend fun syncAll() {
        syncOrders()
        syncProducts()
        syncInventory()
        syncTables()
    }
    private suspend fun syncOrders() {
        val pending = orderRepo.getPending()

        pending.forEach { order ->
            firebaseOrder.createOrder(order.toDomain())
            orderRepo.updateStatus(order.id, "SYNCED")
        }
    }
    private suspend fun syncProducts() {
        val pending = productRepo.getPending()

        pending.forEach { product ->
            firebaseProduct.createProduct(product.toDomain())
            productRepo.updateStatus(product.id, "SYNCED")
        }
    }
    private suspend fun syncInventory() {
        val pending = inventoryRepo.getPending()

        pending.forEach { item ->
            firebaseInventory.updateInventory(item.toDomain())
            inventoryRepo.updateStatus(item.productId, "SYNCED")
        }
    }
    private suspend fun syncTables() {
        val pending = tableRepo.getPending()

        pending.forEach { table ->
            firebaseTable.updateTable(table.toDomain())
            tableRepo.updateStatus(table.id, "SYNCED")
        }
    }
}
package com.laprevia.restobar.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.domain.repository.FirebaseOrderRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import com.laprevia.restobar.di.OrdersReference

@Singleton
class FirebaseOrderRepositoryImpl @Inject constructor(
    @OrdersReference
    private val ordersRef: DatabaseReference
) : FirebaseOrderRepository {

    override fun getOrders(): Flow<List<Order>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = snapshot.children.mapNotNull { it.toOrder() }
                trySend(orders)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ordersRef.addValueEventListener(eventListener)
        awaitClose { ordersRef.removeEventListener(eventListener) }
    }

    // ✅ NUEVO MÉTODO: getOrdersList (suspending)
    override suspend fun getOrdersList(): List<Order> {
        return try {
            val snapshot = ordersRef.get().await()
            val orders = snapshot.children.mapNotNull { it.toOrder() }
            println("📦 FirebaseOrders: ${orders.size} órdenes obtenidas")
            orders
        } catch (e: Exception) {
            println("❌ FirebaseOrders: Error getOrdersList: ${e.message}")
            emptyList()
        }
    }

    override fun getActiveOrders(): Flow<List<Order>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = snapshot.children.mapNotNull { it.toOrder() }
                    .filter { it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED }
                trySend(orders)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ordersRef.addValueEventListener(eventListener)
        awaitClose { ordersRef.removeEventListener(eventListener) }
    }

    override fun getActiveOrdersWithItems(): Flow<List<Order>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = snapshot.children.mapNotNull { it.toOrder() }
                    .filter {
                        it.status != OrderStatus.COMPLETED &&
                                it.status != OrderStatus.CANCELLED
                    }
                trySend(orders)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ordersRef.addValueEventListener(eventListener)
        awaitClose { ordersRef.removeEventListener(eventListener) }
    }

    override fun getOrdersWithItems(): Flow<List<Order>> = getOrders()

    override fun getPendingOrders(): Flow<List<Order>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = snapshot.children.mapNotNull { it.toOrder() }
                    .filter { it.status == OrderStatus.PENDING }
                timber.log.Timber.d("⏳ FirebaseOrders: ${orders.size} órdenes pendientes")
                trySend(orders)
            }

            override fun onCancelled(error: DatabaseError) {
                timber.log.Timber.d("❌ FirebaseOrders: Error en getPendingOrders: ${error.message}")
                close(error.toException())
            }
        }

        ordersRef.orderByChild("status").equalTo("PENDING")
            .addValueEventListener(eventListener)

        awaitClose { ordersRef.removeEventListener(eventListener) }
    }

    override suspend fun createOrder(order: Order) {
        try {
            val orderMap = order.toFirebaseMap()
            ordersRef.child(order.id).setValue(orderMap).await()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun updateOrderStatus(orderId: String, status: String) {
        try {
            val updates = mapOf<String, Any>(
                "status" to status,
                "updatedAt" to System.currentTimeMillis()
            )
            ordersRef.child(orderId).updateChildren(updates).await()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getOrderById(orderId: String): Order? {
        return try {
            val snapshot = ordersRef.child(orderId).get().await()
            snapshot.toOrder()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getOrdersByTable(tableId: Int): List<Order> {
        return try {
            val snapshot = ordersRef.orderByChild("tableId")
                .equalTo(tableId.toDouble())
                .get().await()
            snapshot.children.mapNotNull { it.toOrder() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun deleteOrder(orderId: String) {
        try {
            ordersRef.child(orderId).removeValue().await()
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun updateOrder(order: Order) {
        try {
            val orderMap = order.toFirebaseMap()
            ordersRef.child(order.id).updateChildren(orderMap).await()
        } catch (e: Exception) {
            throw e
        }
    }

    override fun getOrdersByStatus(status: String): Flow<List<Order>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val orders = snapshot.children.mapNotNull { it.toOrder() }
                    .filter { it.status.name == status }
                trySend(orders)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ordersRef.orderByChild("status").equalTo(status)
            .addValueEventListener(eventListener)

        awaitClose { ordersRef.removeEventListener(eventListener) }
    }

    override fun listenToNewOrders(): Flow<Order> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    child.toOrder()?.let { order ->
                        trySend(order)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ordersRef.orderByChild("createdAt")
            .startAt(System.currentTimeMillis().toDouble())
            .addValueEventListener(eventListener)

        awaitClose { ordersRef.removeEventListener(eventListener) }
    }

    override fun listenToOrderChanges(): Flow<Order> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    child.toOrder()?.let { order ->
                        trySend(order)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        ordersRef.addValueEventListener(eventListener)
        awaitClose { ordersRef.removeEventListener(eventListener) }
    }

    override fun getOrdersRealTime(): Flow<List<Order>> = getOrders()

    override suspend fun syncPendingOrders() {
        try {
            timber.log.Timber.d("🔄 FirebaseOrders: Sincronizando órdenes pendientes...")

            val snapshot = ordersRef.orderByChild("status").equalTo("PENDING").get().await()
            val pendingOrders = snapshot.children.mapNotNull { it.toOrder() }

            if (pendingOrders.isNotEmpty()) {
                timber.log.Timber.d("📤 FirebaseOrders: ${pendingOrders.size} órdenes pendientes encontradas")
                pendingOrders.forEach { order ->
                    timber.log.Timber.d("   - Orden ${order.id}: Mesa ${order.tableNumber}, Estado: ${order.status}")
                }
            } else {
                timber.log.Timber.d("✅ FirebaseOrders: No hay órdenes pendientes")
            }
        } catch (e: Exception) {
            timber.log.Timber.d("❌ FirebaseOrders: Error en syncPendingOrders: ${e.message}")
            throw e
        }
    }

    // ==================== EXTENSION FUNCTIONS ====================

    private fun DataSnapshot.toOrder(): Order? {
        return try {
            val id = key ?: return null
            // ✅ CORREGIDO: tableId como var para poder modificarlo
            var tableId = child("tableId").getValue(Int::class.java) ?: 0
            val tableNumber = child("tableNumber").getValue(Int::class.java) ?: 0

            // ✅ CORREGIDO: Si tableId es 0, usar tableNumber (mesas del 1 al 8)
            if (tableId == 0 && tableNumber in 1..8) {
                println("⚠️ FirebaseOrders: tableId era 0, corrigiendo a $tableNumber")
                tableId = tableNumber
            }

            val statusStr = child("status").getValue(String::class.java) ?: "PENDING"
            val status = try {
                OrderStatus.valueOf(statusStr)
            } catch (e: IllegalArgumentException) {
                when (statusStr) {
                    "ENVIADO" -> OrderStatus.ENVIADO
                    "ACEPTADO" -> OrderStatus.ACEPTADO
                    "EN_PREPARACION" -> OrderStatus.EN_PREPARACION
                    "LISTO" -> OrderStatus.LISTO
                    "ENTREGADO" -> OrderStatus.ENTREGADO
                    "COMPLETED" -> OrderStatus.COMPLETED
                    "CANCELLED" -> OrderStatus.CANCELLED
                    else -> OrderStatus.PENDING
                }
            }
            val waiterId = child("waiterId").getValue(String::class.java)
            val waiterName = child("waiterName").getValue(String::class.java)
            val createdAt = child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
            val updatedAt = child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
            val notes = child("notes").getValue(String::class.java)
            val total = child("total").getValue(Double::class.java) ?: 0.0

            val items = mutableListOf<com.laprevia.restobar.data.model.OrderItem>()
            val itemsSnapshot = child("items")

            if (itemsSnapshot.exists()) {
                itemsSnapshot.children.forEach { itemSnapshot ->
                    try {
                        val productId = itemSnapshot.child("productId").getValue(String::class.java) ?: ""
                        val productName = itemSnapshot.child("productName").getValue(String::class.java) ?: "Producto"
                        val productDescription = itemSnapshot.child("productDescription").getValue(String::class.java) ?: ""
                        val productCategory = itemSnapshot.child("productCategory").getValue(String::class.java) ?: ""
                        val quantity = itemSnapshot.child("quantity").getValue(Int::class.java) ?: 0
                        val unitPrice = itemSnapshot.child("unitPrice").getValue(Double::class.java) ?: 0.0
                        val subtotal = itemSnapshot.child("subtotal").getValue(Double::class.java) ?: 0.0
                        val trackInventory = itemSnapshot.child("trackInventory").getValue(Boolean::class.java) ?: false

                        val orderItem = com.laprevia.restobar.data.model.OrderItem(
                            productId = productId,
                            productName = productName,
                            productDescription = productDescription,
                            productCategory = productCategory,
                            quantity = quantity,
                            unitPrice = unitPrice,
                            subtotal = subtotal,
                            trackInventory = trackInventory
                        )

                        items.add(orderItem)
                    } catch (e: Exception) {
                        timber.log.Timber.d("❌ Error leyendo item: ${e.message}")
                    }
                }
            }

            Order(
                id = id,
                tableId = tableId,
                tableNumber = tableNumber,
                items = items,
                status = status,
                createdAt = createdAt,
                updatedAt = updatedAt,
                total = total,
                waiterId = waiterId,
                waiterName = waiterName,
                notes = notes
            )
        } catch (e: Exception) {
            timber.log.Timber.d("❌ Error convirtiendo DataSnapshot a Order: ${e.message}")
            null
        }
    }

    private fun Order.toFirebaseMap(): Map<String, Any> {
        val itemsList = items.map { item ->
            mapOf<String, Any>(
                "productId" to item.productId,
                "productName" to item.productName,
                "productDescription" to item.productDescription,
                "productCategory" to item.productCategory,
                "quantity" to item.quantity,
                "unitPrice" to item.unitPrice,
                "subtotal" to item.subtotal,
                "trackInventory" to item.trackInventory
            )
        }

        return mapOf<String, Any>(
            "id" to id,
            "tableId" to tableId,
            "tableNumber" to tableNumber,
            "items" to itemsList,
            "status" to status.name,
            "waiterId" to (waiterId ?: ""),
            "waiterName" to (waiterName ?: ""),
            "createdAt" to createdAt,
            "updatedAt" to updatedAt,
            "notes" to (notes ?: ""),
            "total" to total
        )
    }
}

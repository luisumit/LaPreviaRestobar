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

@Singleton
class FirebaseOrderRepositoryImpl @Inject constructor(
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

    override suspend fun createOrder(order: Order) {
        try {
            // ✅ CORREGIDO: Usar el ID que ya viene en la orden
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

    // ✅ AGREGADO: Método faltante de OrderRepository
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

    // Métodos específicos para comunicación en tiempo real

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

        // ✅ CORREGIDO: Usar startAt con Double en lugar de Long
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

    // 🔧🔧🔧 EXTENSION FUNCTIONS ACTUALIZADAS 🔧🔧🔧

    private fun DataSnapshot.toOrder(): Order? {
        return try {
            val id = key ?: return null
            val tableId = child("tableId").getValue(Int::class.java) ?: 0
            val tableNumber = child("tableNumber").getValue(Int::class.java) ?: 0
            val statusStr = child("status").getValue(String::class.java) ?: "PENDING"
            val status = OrderStatus.valueOf(statusStr)
            val waiterId = child("waiterId").getValue(String::class.java) ?: ""
            val waiterName = child("waiterName").getValue(String::class.java) ?: ""
            val createdAt = child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
            val updatedAt = child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()
            val notes = child("notes").getValue(String::class.java) ?: ""
            val total = child("total").getValue(Double::class.java) ?: 0.0

            // ✅✅✅ CORREGIDO: Leer los items de Firebase
            val items = mutableListOf<com.laprevia.restobar.data.model.OrderItem>()
            val itemsSnapshot = child("items")

            if (itemsSnapshot.exists()) {
                println("📦 Firebase: Leyendo ${itemsSnapshot.children.count()} items para orden $id")

                itemsSnapshot.children.forEachIndexed { index, itemSnapshot ->
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
                        println("   - Item $index: $quantity x $productName - S/.$subtotal")

                    } catch (e: Exception) {
                        println("❌ Error leyendo item $index: ${e.message}")
                    }
                }
            } else {
                println("⚠️ Firebase: Orden $id no tiene campo 'items'")
            }

            Order(
                id = id,
                tableId = tableId,
                tableNumber = tableNumber,
                items = items, // ✅✅✅ AHORA SÍ con los items leídos
                status = status,
                createdAt = createdAt,
                updatedAt = updatedAt,
                total = total,
                waiterId = waiterId,
                waiterName = waiterName,
                notes = notes
            )
        } catch (e: Exception) {
            println("❌ Error convirtiendo DataSnapshot a Order: ${e.message}")
            null
        }
    }

    private fun Order.toFirebaseMap(): Map<String, Any> {
        // ✅✅✅ CORREGIDO: Convertir items a formato Firebase
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

        println("🔥 Firebase: Guardando orden con ${items.size} items")
        items.forEachIndexed { index, item ->
            println("   - Item $index: ${item.quantity}x ${item.productName}")
        }

        return mapOf<String, Any>(
            "id" to id,
            "tableId" to tableId,
            "tableNumber" to tableNumber,
            "items" to itemsList, // ✅✅✅ AHORA SÍ guardando los items
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
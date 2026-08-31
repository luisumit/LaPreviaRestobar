package com.laprevia.restobar.data.repository

import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.PaymentMethod
import com.laprevia.restobar.domain.repository.FirebaseOrderRepository
import com.laprevia.restobar.platform.currentTimeMillis
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.DataSnapshot
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

/**
 * Repositorio de pedidos MULTIPLATAFORMA (GitLive Firebase): Android, Desktop/JVM y Web JS.
 * Es el adaptador mas critico del sistema (flujo mozo->cocina->cobro en tiempo real).
 *
 * Conserva EXACTAMENTE el mismo formato de datos y la misma semantica que la
 * implementacion nativa: normalizacion de tableId, estados legacy, items anidados
 * y campos de pago/descuento. La base en produccion ya tiene datos.
 */
class GitLiveOrderRepository : FirebaseOrderRepository {

    private val ordersRef: DatabaseReference get() = Firebase.database.reference("orders")

    // ==================== LISTADOS / TIEMPO REAL ====================

    override fun getOrders(): Flow<List<Order>> =
        ordersRef.valueEvents.map { snapshot -> snapshot.children.mapNotNull { it.toOrder() } }

    override fun getOrdersWithItems(): Flow<List<Order>> = getOrders()

    override fun getOrdersRealTime(): Flow<List<Order>> = getOrders()

    override fun getActiveOrders(): Flow<List<Order>> =
        getOrders().map { orders ->
            orders.filter { it.status != OrderStatus.COMPLETED && it.status != OrderStatus.CANCELLED }
        }

    override fun getActiveOrdersWithItems(): Flow<List<Order>> = getActiveOrders()

    override fun getOrdersByStatus(status: String): Flow<List<Order>> =
        ordersRef.orderByChild("status").equalTo(status).valueEvents
            .map { snapshot ->
                snapshot.children.mapNotNull { it.toOrder() }.filter { it.status.name == status }
            }

    override fun getPendingOrders(): Flow<List<Order>> =
        ordersRef.orderByChild("status").equalTo("PENDING").valueEvents
            .map { snapshot ->
                snapshot.children.mapNotNull { it.toOrder() }
                    .filter { it.status == OrderStatus.PENDING }
            }

    /** Igual que la nativa: solo pedidos creados despues de suscribirse. */
    override fun listenToNewOrders(): Flow<Order> =
        ordersRef.orderByChild("createdAt").startAt(currentTimeMillis().toDouble()).valueEvents
            .transform { snapshot ->
                snapshot.children.forEach { child -> child.toOrder()?.let { emit(it) } }
            }

    override fun listenToOrderChanges(): Flow<Order> =
        ordersRef.valueEvents.transform { snapshot ->
            snapshot.children.forEach { child -> child.toOrder()?.let { emit(it) } }
        }

    // ==================== BUSQUEDA ====================

    override suspend fun getOrderById(orderId: String): Order? = runCatching {
        ordersRef.child(orderId).valueEvents.first().toOrder()
    }.getOrNull()

    override suspend fun getOrdersByTable(tableId: Int): List<Order> = runCatching {
        ordersRef.orderByChild("tableId").equalTo(tableId.toDouble()).valueEvents.first()
            .children.mapNotNull { it.toOrder() }
    }.getOrDefault(emptyList())

    override suspend fun getOrdersList(): List<Order> = runCatching {
        ordersRef.valueEvents.first().children.mapNotNull { it.toOrder() }
    }.getOrDefault(emptyList())

    // ==================== CRUD ====================

    override suspend fun createOrder(order: Order) {
        ordersRef.child(order.id).updateChildren(order.toFirebaseMap())
    }

    override suspend fun updateOrder(order: Order) {
        ordersRef.child(order.id).updateChildren(order.toFirebaseMap())
    }

    // Actualiza SOLO items/total/updatedAt: NO toca el status para no revertir un
    // cambio de estado hecho por la cocina en paralelo.
    override suspend fun updateOrderItems(orderId: String, items: List<OrderItem>, total: Double) {
        val itemsList = items.map { item ->
            mapOf<String, Any?>(
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
        ordersRef.child(orderId).updateChildren(
            mapOf(
                "items" to itemsList,
                "total" to total,
                "updatedAt" to currentTimeMillis()
            )
        )
    }

    override suspend fun deleteOrder(orderId: String) {
        ordersRef.child(orderId).removeValue()
    }

    override suspend fun updateOrderStatus(orderId: String, status: String) {
        ordersRef.child(orderId).updateChildren(
            mapOf("status" to status, "updatedAt" to currentTimeMillis())
        )
    }

    override suspend fun updateOrderPayment(
        orderId: String,
        status: String,
        paymentMethod: String,
        paidAt: Long,
        receiptNumber: String,
        amountReceived: Double,
        changeGiven: Double
    ) {
        ordersRef.child(orderId).updateChildren(
            mapOf(
                "status" to status,
                "paymentMethod" to paymentMethod,
                "paidAt" to paidAt,
                "receiptNumber" to receiptNumber,
                "amountReceived" to amountReceived,
                "changeGiven" to changeGiven,
                "updatedAt" to currentTimeMillis()
            )
        )
    }

    override suspend fun syncPendingOrders() {
        // Igual que la nativa: solo inspecciona pendientes (log-only).
        runCatching {
            ordersRef.orderByChild("status").equalTo("PENDING").valueEvents.first()
        }
    }

    // ==================== CONVERSION (mismo wire format que el SDK nativo) ====================

    private fun DataSnapshot.toOrder(): Order? = runCatching {
        val id = key ?: return null
        var tableId = (child("tableId").value as? Number)?.toInt() ?: 0
        val tableNumber = (child("tableNumber").value as? Number)?.toInt() ?: 0
        // Normalizacion legacy: si tableId es 0, usar tableNumber (mesas 1..8)
        if (tableId == 0 && tableNumber in 1..8) tableId = tableNumber

        val statusStr = child("status").value as? String ?: "PENDING"
        val status = OrderStatus.valueOfOrNull(statusStr) ?: OrderStatus.fromString(statusStr)

        val items = child("items").children.mapNotNull { itemSnapshot ->
            runCatching {
                OrderItem(
                    productId = itemSnapshot.child("productId").value as? String ?: "",
                    productName = itemSnapshot.child("productName").value as? String ?: "Producto",
                    productDescription = itemSnapshot.child("productDescription").value as? String ?: "",
                    productCategory = itemSnapshot.child("productCategory").value as? String ?: "",
                    quantity = (itemSnapshot.child("quantity").value as? Number)?.toInt() ?: 0,
                    unitPrice = (itemSnapshot.child("unitPrice").value as? Number)?.toDouble() ?: 0.0,
                    subtotal = (itemSnapshot.child("subtotal").value as? Number)?.toDouble() ?: 0.0,
                    trackInventory = itemSnapshot.child("trackInventory").value as? Boolean ?: false
                )
            }.getOrNull()
        }

        Order(
            id = id,
            tableId = tableId,
            tableNumber = tableNumber,
            items = items,
            status = status,
            createdAt = (child("createdAt").value as? Number)?.toLong() ?: currentTimeMillis(),
            updatedAt = (child("updatedAt").value as? Number)?.toLong() ?: currentTimeMillis(),
            total = (child("total").value as? Number)?.toDouble() ?: 0.0,
            waiterId = child("waiterId").value as? String,
            waiterName = child("waiterName").value as? String,
            notes = child("notes").value as? String,
            paymentMethod = PaymentMethod.fromString(child("paymentMethod").value as? String),
            paidAt = (child("paidAt").value as? Number)?.toLong()?.takeIf { it > 0 },
            receiptNumber = (child("receiptNumber").value as? String)?.takeIf { it.isNotBlank() },
            amountReceived = (child("amountReceived").value as? Number)?.toDouble()?.takeIf { it > 0.0 },
            changeGiven = (child("changeGiven").value as? Number)?.toDouble()?.takeIf { it > 0.0 },
            discountAmount = (child("discountAmount").value as? Number)?.toDouble()?.takeIf { it > 0.0 },
            discountReason = (child("discountReason").value as? String)?.takeIf { it.isNotBlank() }
        )
    }.getOrNull()

    private fun Order.toFirebaseMap(): Map<String, Any?> {
        val itemsList = items.map { item ->
            mapOf<String, Any?>(
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
        return mapOf(
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
            "total" to total,
            "paymentMethod" to paymentMethod.name,
            "paidAt" to (paidAt ?: 0L),
            "receiptNumber" to (receiptNumber ?: ""),
            "amountReceived" to (amountReceived ?: 0.0),
            "changeGiven" to (changeGiven ?: 0.0),
            "discountAmount" to (discountAmount ?: 0.0),
            "discountReason" to (discountReason ?: "")
        )
    }
}

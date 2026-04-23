package com.laprevia.restobar.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.domain.repository.FirebaseInventoryRepository
import com.laprevia.restobar.domain.repository.FirebaseOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChefViewModel @Inject constructor(
    private val firebaseOrderRepository: FirebaseOrderRepository,
    private val firebaseInventoryRepository: FirebaseInventoryRepository
) : ViewModel() {

    // StateFlows principales
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

    // Estado de conexión Firebase
    private val _isFirebaseConnected = MutableStateFlow(false)
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    // Mensajes y notificaciones
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _notifications = MutableStateFlow<List<ChefNotification>>(emptyList())
    val notifications: StateFlow<List<ChefNotification>> = _notifications.asStateFlow()

    init {
        println("👨‍🍳 ChefViewModel INICIADO - Solo Firebase")
        initializeFirebase()
        loadOrders()
        setupFirebaseRealtimeUpdates()
    }

    // ==================== INICIALIZACIÓN FIREBASE ====================

    private fun initializeFirebase() {
        viewModelScope.launch {
            try {
                // Inicializar inventario por defecto en Firebase
                firebaseInventoryRepository.initializeDefaultInventory()
                println("✅ Firebase: Inventario inicializado")
                _isFirebaseConnected.value = true
            } catch (e: Exception) {
                println("❌ Firebase: Error inicializando: ${e.message}")
                _isFirebaseConnected.value = false
            }
        }
    }

    // ==================== FIREBASE REAL-TIME UPDATES ====================

    private fun setupFirebaseRealtimeUpdates() {
        viewModelScope.launch {
            try {
                // Escuchar NUEVAS órdenes del mesero en tiempo real
                firebaseOrderRepository.listenToNewOrders().collect { newOrder ->
                    println("🎯 Firebase: ¡NUEVA ORDEN DEL MESERO! - Mesa ${newOrder.tableNumber}")
                    handleNewOrderFromFirebase(newOrder)
                }
            } catch (e: Exception) {
                println("❌ Firebase: Error en escucha nuevas órdenes: ${e.message}")
                _isFirebaseConnected.value = false
            }
        }

        viewModelScope.launch {
            try {
                // Escuchar cambios en órdenes existentes
                firebaseOrderRepository.listenToOrderChanges().collect { updatedOrder ->
                    println("🔄 Firebase: Orden actualizada - ${updatedOrder.id}")
                    handleUpdatedOrderFromFirebase(updatedOrder)
                }
            } catch (e: Exception) {
                println("❌ Firebase: Error en escucha de cambios: ${e.message}")
            }
        }
    }

    private fun handleNewOrderFromFirebase(newOrder: Order) {
        println("🎯 Chef: Procesando orden de Firebase - Mesa ${newOrder.tableNumber}, Estado: ${newOrder.status}")

        val existingOrder = _orders.value.find { it.id == newOrder.id }

        if (existingOrder == null) {
            // NUEVA orden - agregar a la lista
            _orders.value = _orders.value + newOrder
            println("✅ Chef: NUEVA ORDEN AGREGADA - Mesa ${newOrder.tableNumber}")

            // Mostrar notificación SOLO si es orden nueva enviada por mesero
            if (newOrder.status == OrderStatus.ENVIADO) {
                showNewOrderNotification(newOrder)
                println("🔔 Chef: NOTIFICACIÓN NUEVA ORDEN - Mesa ${newOrder.tableNumber}")
            }
        } else {
            // ACTUALIZAR orden existente - CORREGIDO
            val updatedOrders = _orders.value.map { order ->
                if (order.id == newOrder.id) newOrder else order
            }
            _orders.value = updatedOrders

            // Notificar cambio de estado
            if (existingOrder.status != newOrder.status) {
                println("🔄 Chef: ORDEN ACTUALIZADA - Mesa ${newOrder.tableNumber}: ${existingOrder.status} -> ${newOrder.status}")
                showStatusChangeNotification(existingOrder, newOrder)
            }
        }

        // DEBUG: Mostrar estado actual de órdenes
        println("📊 Chef: Estado actual - ${_orders.value.size} órdenes:")
        _orders.value.forEach { order ->
            println("   - Mesa ${order.tableNumber}: ${order.status} (${order.items.size} items)")
        }
    }

    private fun handleUpdatedOrderFromFirebase(updatedOrder: Order) {
        println("🔄 Chef: Procesando actualización de Firebase - Mesa ${updatedOrder.tableNumber}, Estado: ${updatedOrder.status}")

        val currentOrders = _orders.value.toMutableList()
        val existingIndex = currentOrders.indexOfFirst { it.id == updatedOrder.id }

        if (existingIndex != -1) {
            val previousOrder = currentOrders[existingIndex]
            currentOrders[existingIndex] = updatedOrder
            _orders.value = currentOrders

            // Notificar cambio de estado
            if (previousOrder.status != updatedOrder.status) {
                println("🔍 Chef: Cambio de estado - ${updatedOrder.id}: ${previousOrder.status} -> ${updatedOrder.status}")
                showStatusChangeNotification(previousOrder, updatedOrder)
            }

            // Actualizar orden seleccionada si es la misma
            if (_selectedOrder.value?.id == updatedOrder.id) {
                _selectedOrder.value = updatedOrder
                println("🎯 Chef: Orden seleccionada actualizada")
            }
        } else {
            // Si no existe, agregarla
            _orders.value = currentOrders + updatedOrder
            println("➕ Chef: ORDEN AGREGADA (no existía) - Mesa ${updatedOrder.tableNumber}")
        }

        // DEBUG: Mostrar estado actual después de actualización
        println("📊 Chef: Estado después de actualización - ${_orders.value.size} órdenes")
    }

    // ==================== CARGA DE DATOS ====================

    private fun loadOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Cargar órdenes activas desde Firebase
                firebaseOrderRepository.getActiveOrders().collect { firebaseOrders ->
                    println("🔥 ChefViewModel: ${firebaseOrders.size} órdenes recibidas de Firebase")

                    // Logs detallados para diagnóstico
                    firebaseOrders.forEachIndexed { index, order ->
                        println("   Orden $index: ID=${order.id}, Mesa=${order.tableNumber}, Estado=${order.status}, Items=${order.items.size}")
                        if (order.items.isNotEmpty()) {
                            order.items.forEachIndexed { itemIndex, item ->
                                println("     Item $itemIndex: ${item.quantity}x ${item.productName} - S/.${item.subtotal}")
                            }
                        } else {
                            println("     ⚠️ SIN ITEMS EN ESTA ORDEN")
                        }
                    }

                    _orders.value = firebaseOrders
                    _isLoading.value = false
                    _isFirebaseConnected.value = true
                    println("✅ Firebase: ${firebaseOrders.size} órdenes activas cargadas")
                }
            } catch (e: Exception) {
                println("❌ Firebase: Error cargando órdenes: ${e.message}")
                _errorMessage.value = "Error cargando órdenes: ${e.message}"
                _isLoading.value = false
                _isFirebaseConnected.value = false
            }
        }
    }

    // ==================== GESTIÓN DE ESTADOS DE ÓRDENES ====================

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            try {
                println("🔄 Chef: Actualizando orden $orderId a $status")

                // ✅ SOLO actualizar si el estado es válido para el chef
                val validChefStates = listOf(
                    OrderStatus.ACEPTADO,
                    OrderStatus.EN_PREPARACION,
                    OrderStatus.LISTO,
                    OrderStatus.COMPLETED,
                    OrderStatus.CANCELLED
                )

                if (status !in validChefStates) {
                    println("⚠️ Chef: Estado $status no permitido para chef")
                    return@launch
                }

                val statusString = when (status) {
                    OrderStatus.ACEPTADO -> "ACEPTADO"
                    OrderStatus.EN_PREPARACION -> "EN_PREPARACION"
                    OrderStatus.LISTO -> "LISTO"
                    OrderStatus.COMPLETED -> "COMPLETED"
                    OrderStatus.CANCELLED -> "CANCELLED"
                    else -> return@launch // No permitir otros estados
                }

                // ✅ SOLO Firebase - sin WebSocket
                firebaseOrderRepository.updateOrderStatus(orderId, statusString)

                // Actualizar inventario si es necesario
                if (status == OrderStatus.EN_PREPARACION) {
                    updateInventoryForOrder(orderId)
                }

                val order = _orders.value.find { it.id == orderId }
                if (order != null) {
                    _successMessage.value = when (status) {
                        OrderStatus.ACEPTADO -> "✅ Orden aceptada - Mesa ${order.tableNumber}"
                        OrderStatus.EN_PREPARACION -> "👨‍🍳 Orden en preparación - Mesa ${order.tableNumber}"
                        OrderStatus.LISTO -> "🎉 ¡Orden lista! - Mesa ${order.tableNumber}"
                        OrderStatus.COMPLETED -> "✅ Orden completada - Mesa ${order.tableNumber}"
                        OrderStatus.CANCELLED -> "❌ Orden cancelada - Mesa ${order.tableNumber}"
                        else -> "Estado actualizado - Mesa ${order.tableNumber}"
                    }
                }

                println("✅ Firebase: Orden $orderId actualizada a $status")

            } catch (e: Exception) {
                _errorMessage.value = "❌ Error actualizando orden: ${e.message}"
                println("❌ Chef: Error actualizando orden: ${e.message}")
            }
        }
    }

    // ==================== GESTIÓN DE INVENTARIO ====================

    private suspend fun updateInventoryForOrder(orderId: String) {
        val order = _orders.value.find { it.id == orderId }
        if (order == null) {
            println("⚠️ Chef: Orden $orderId no encontrada para actualizar inventario")
            return
        }

        println("📦 Chef: Actualizando inventario para orden ${order.id} - ${order.items.size} items")

        order.items.forEach { item ->
            if (item.trackInventory) {
                try {
                    val currentStock = firebaseInventoryRepository.getCurrentStock(item.productId)
                    val newStock = currentStock - item.quantity

                    if (newStock >= 0) {
                        firebaseInventoryRepository.updateStock(item.productId, newStock)
                        println("📦 Inventario: Stock de ${item.productName} actualizado a $newStock")

                        addNotification(
                            ChefNotification(
                                type = ChefNotificationType.ORDER_IN_PREPARATION,
                                title = "📦 Inventario Actualizado",
                                message = "${item.productName}: ${currentStock.toInt()} → ${newStock.toInt()}",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    } else {
                        println("⚠️ Inventario: Stock insuficiente para ${item.productName}")
                        _errorMessage.value = "Stock insuficiente: ${item.productName}"

                        addNotification(
                            ChefNotification(
                                type = ChefNotificationType.ORDER_CANCELLED,
                                title = "⚠️ Stock Insuficiente",
                                message = "${item.productName}: Solo hay ${currentStock.toInt()} unidades",
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                } catch (e: Exception) {
                    println("❌ Inventario: Error actualizando ${item.productName}: ${e.message}")
                    _errorMessage.value = "Error actualizando inventario: ${e.message}"
                }
            } else {
                println("📦 Inventario: ${item.productName} no requiere control de stock")
            }
        }
    }

    // Métodos de conveniencia
    fun acceptOrder(orderId: String) {
        println("✅ Chef: Aceptando orden $orderId")
        updateOrderStatus(orderId, OrderStatus.ACEPTADO)
    }

    fun startOrderPreparation(orderId: String) {
        println("👨‍🍳 Chef: Iniciando preparación de orden $orderId")
        updateOrderStatus(orderId, OrderStatus.EN_PREPARACION)
    }

    fun markOrderAsReady(orderId: String) {
        println("🎉 Chef: Marcando orden $orderId como lista")
        updateOrderStatus(orderId, OrderStatus.LISTO)
    }

    fun completeOrder(orderId: String) {
        println("✅ Chef: Completando orden $orderId")
        updateOrderStatus(orderId, OrderStatus.COMPLETED)
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            try {
                val order = _orders.value.find { it.id == orderId }
                if (order != null) {
                    firebaseOrderRepository.deleteOrder(orderId)
                    _successMessage.value = "Orden cancelada - Mesa ${order.tableNumber}"

                    if (_selectedOrder.value?.id == orderId) {
                        clearOrderSelection()
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error cancelando orden: ${e.message}"
            }
        }
    }

    // ==================== NOTIFICACIONES ====================

    private fun showNewOrderNotification(order: Order) {
        println("🎯 Chef: ¡NUEVA ORDEN DEL MESERO!")
        println("   - Mesa: ${order.tableNumber}")
        println("   - ID: ${order.id}")
        println("   - Items: ${order.items.size}")
        println("   - Total: S/. ${order.total}")

        // Mostrar detalles de items en notificación
        if (order.items.isNotEmpty()) {
            println("   - Detalles de items:")
            order.items.forEachIndexed { index, item ->
                println("     ${index + 1}. ${item.quantity}x ${item.productName} - S/. ${item.subtotal}")
            }
        }

        addNotification(
            ChefNotification(
                type = ChefNotificationType.NEW_ORDER,
                title = "🆕 Nueva Orden - Mesa ${order.tableNumber}",
                message = "Nuevo pedido con ${order.items.size} items - Total: S/. ${"%.2f".format(order.total)}",
                orderId = order.id,
                tableNumber = order.tableNumber,
                itemsCount = order.items.size,
                totalAmount = order.total
            )
        )
    }

    private fun showStatusChangeNotification(previous: Order, current: Order) {
        val notification = when (current.status) {
            OrderStatus.ACEPTADO -> ChefNotification(
                type = ChefNotificationType.ORDER_ACCEPTED,
                title = "✅ Orden Aceptada - Mesa ${current.tableNumber}",
                message = "La orden ha sido aceptada para preparación",
                orderId = current.id,
                tableNumber = current.tableNumber
            )
            OrderStatus.EN_PREPARACION -> ChefNotification(
                type = ChefNotificationType.ORDER_IN_PREPARATION,
                title = "👨‍🍳 En Preparación - Mesa ${current.tableNumber}",
                message = "La orden está siendo preparada",
                orderId = current.id,
                tableNumber = current.tableNumber
            )
            OrderStatus.LISTO -> ChefNotification(
                type = ChefNotificationType.ORDER_READY,
                title = "🎉 ¡Orden Lista! - Mesa ${current.tableNumber}",
                message = "La orden está lista para servir",
                orderId = current.id,
                tableNumber = current.tableNumber
            )
            OrderStatus.COMPLETED -> ChefNotification(
                type = ChefNotificationType.ORDER_READY,
                title = "✅ Orden Completada - Mesa ${current.tableNumber}",
                message = "La orden ha sido completada",
                orderId = current.id,
                tableNumber = current.tableNumber
            )
            OrderStatus.CANCELLED -> ChefNotification(
                type = ChefNotificationType.ORDER_CANCELLED,
                title = "❌ Orden Cancelada - Mesa ${current.tableNumber}",
                message = "La orden ha sido cancelada",
                orderId = current.id,
                tableNumber = current.tableNumber
            )
            else -> null
        }

        notification?.let {
            addNotification(it)
            println("🔔 Chef: Notificación de cambio de estado - ${it.title}")
        }
    }

    private fun addNotification(notification: ChefNotification) {
        _notifications.value = listOf(notification) + _notifications.value.take(4)
        println("🔔 Chef: Notificación agregada - ${notification.title}")
    }

    fun removeNotification(notification: ChefNotification) {
        _notifications.value = _notifications.value.filter { it != notification }
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }

    fun selectOrder(order: Order) {
        _selectedOrder.value = order
        println("🎯 Chef: Orden ${order.id} seleccionada - Mesa ${order.tableNumber}")
    }

    fun clearOrderSelection() {
        _selectedOrder.value = null
    }

    fun refreshOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            loadOrders()
        }
    }

    fun getOrderById(orderId: String): Order? {
        return _orders.value.find { it.id == orderId }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    // Propiedades computadas
    val newOrdersCount: Int get() = _orders.value.count { it.status == OrderStatus.ENVIADO }
    val acceptedOrdersCount: Int get() = _orders.value.count { it.status == OrderStatus.ACEPTADO }
    val inProgressOrdersCount: Int get() = _orders.value.count { it.status == OrderStatus.EN_PREPARACION }
    val readyOrdersCount: Int get() = _orders.value.count { it.status == OrderStatus.LISTO }
    val totalActiveOrders: Int get() = _orders.value.size

    // Estado de conexión para la UI
    val connectionStatus: String get() =
        if (_isFirebaseConnected.value) "🟢 Conectado a Firebase"
        else "🔴 Sin conexión"

    fun getStatusText(status: OrderStatus): String {
        return when (status) {
            OrderStatus.ENVIADO -> "🆕 Enviado por mesero"
            OrderStatus.ACEPTADO -> "✅ Aceptado"
            OrderStatus.EN_PREPARACION -> "👨‍🍳 En preparación"
            OrderStatus.LISTO -> "🎉 Listo para servir"
            OrderStatus.COMPLETED -> "✅ Completado"
            OrderStatus.CANCELLED -> "❌ Cancelado"
            else -> status.toString()
        }
    }

    // Modelos de notificación
    data class ChefNotification(
        val id: String = System.currentTimeMillis().toString(),
        val type: ChefNotificationType,
        val title: String,
        val message: String,
        val orderId: String? = null,
        val tableNumber: Int? = null,
        val itemsCount: Int = 0,
        val totalAmount: Double = 0.0,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class ChefNotificationType {
        NEW_ORDER, ORDER_ACCEPTED, ORDER_IN_PREPARATION, ORDER_READY, ORDER_CANCELLED
    }
}
package com.laprevia.restobar.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.Product
import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.data.model.TableStatus
import com.laprevia.restobar.domain.repository.FirebaseOrderRepository
import com.laprevia.restobar.domain.repository.FirebaseProductRepository
import com.laprevia.restobar.domain.repository.FirebaseTableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class WaiterViewModel @Inject constructor(
    private val firebaseTableRepository: FirebaseTableRepository,
    private val firebaseOrderRepository: FirebaseOrderRepository,
    private val firebaseProductRepository: FirebaseProductRepository
) : ViewModel() {

    // StateFlows principales
    private val _tables = MutableStateFlow<List<Table>>(emptyList())
    val tables: StateFlow<List<Table>> = _tables.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Estado para conexión Firebase
    private val _isFirebaseConnected = MutableStateFlow(false)
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Conectando...")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    // Estado del pedido actual
    private val _currentOrderItems = MutableStateFlow<List<OrderItem>>(emptyList())
    val currentOrderItems: StateFlow<List<OrderItem>> = _currentOrderItems.asStateFlow()

    private val _currentTableId = MutableStateFlow<Int?>(null)
    val currentTableId: StateFlow<Int?> = _currentTableId.asStateFlow()

    // Mensajes y notificaciones
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    init {
        println("🟢 WaiterViewModel INICIADO - Solo Firebase")
        viewModelScope.launch {
            initializeFirebase()
            delay(1000)
            loadInitialData()
            setupFirebaseRealtimeUpdates()
        }
    }

    // ==================== FIREBASE REAL-TIME UPDATES ====================

    private fun setupFirebaseRealtimeUpdates() {
        viewModelScope.launch {
            try {
                println("🔥 Waiter: Configurando Firebase Real-time Updates...")

                // Escuchar ACTUALIZACIONES del chef via Firebase - CORREGIDO
                firebaseOrderRepository.listenToOrderChanges().collect { updatedOrder ->
                    println("🔄 Waiter: Actualización del CHEF - Orden ${updatedOrder.id}")
                    println("   - Mesa: ${updatedOrder.tableNumber}")
                    println("   - Estado: ${updatedOrder.status}")
                    println("   - Items: ${updatedOrder.items.size}")

                    _isFirebaseConnected.value = true
                    _connectionStatus.value = "🟢 Conectado a cocina"
                    handleUpdatedOrderFromFirebase(updatedOrder)
                }

            } catch (e: Exception) {
                println("❌ Waiter: Error en Firebase Real-time: ${e.message}")
                _isFirebaseConnected.value = false
                _connectionStatus.value = "🔴 Sin conexión con cocina"
            }
        }

        // 🔥 NUEVO: Escuchar también nuevas órdenes (por si acaso)
        viewModelScope.launch {
            try {
                firebaseOrderRepository.listenToNewOrders().collect { newOrder ->
                    println("📥 Waiter: Nueva orden detectada - Mesa ${newOrder.tableNumber}")
                    handleUpdatedOrderFromFirebase(newOrder)
                }
            } catch (e: Exception) {
                println("❌ Waiter: Error escuchando nuevas órdenes: ${e.message}")
            }
        }
    }

    private fun handleUpdatedOrderFromFirebase(updatedOrder: Order) {
        println("🔄 Waiter: Procesando actualización - Mesa ${updatedOrder.tableNumber}, Estado: ${updatedOrder.status}")

        val currentOrders = _orders.value.toMutableList()
        val existingIndex = currentOrders.indexOfFirst { it.id == updatedOrder.id }

        if (existingIndex != -1) {
            val previousOrder = currentOrders[existingIndex]
            currentOrders[existingIndex] = updatedOrder
            _orders.value = currentOrders

            // 🔥 NOTIFICAR al mesero cuando el chef cambia el estado - MEJORADO
            if (previousOrder.status != updatedOrder.status) {
                println("🔔 Waiter: El chef actualizó orden ${updatedOrder.id}")
                println("   - Estado anterior: ${previousOrder.status}")
                println("   - Estado nuevo: ${updatedOrder.status}")
                println("   - Mesa: ${updatedOrder.tableNumber}")

                showStatusChangeNotification(previousOrder, updatedOrder)

                // 🔥 NUEVO: Actualizar también el estado de la mesa si es necesario
                if (updatedOrder.status == OrderStatus.LISTO) {
                    println("🎉 Waiter: ¡Orden LISTA para mesa ${updatedOrder.tableNumber}!")
                }
            }
        } else {
            // Si no existe, agregarla
            _orders.value = currentOrders + updatedOrder
            println("➕ Waiter: Orden agregada desde Firebase - Mesa ${updatedOrder.tableNumber}")
        }

        // DEBUG: Mostrar estado actual de órdenes
        println("📊 Waiter: Estado actual - ${_orders.value.size} órdenes:")
        _orders.value.forEach { order ->
            println("   - Mesa ${order.tableNumber}: ${order.status} (${order.items.size} items)")
        }
    }

    // ==================== CARGA DE DATOS ====================

    private fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                println("🔄 Waiter: Cargando datos iniciales...")

                val tablesJob = launch { loadTables() }
                val productsJob = launch { loadProducts() }
                val ordersJob = launch { loadOrders() }

                tablesJob.join()
                productsJob.join()
                ordersJob.join()

                _isLoading.value = false
                println("✅ Waiter: Datos cargados - ${_tables.value.size} mesas, ${_orders.value.size} órdenes, ${_products.value.size} productos")

            } catch (e: Exception) {
                _errorMessage.value = "Error cargando datos: ${e.message}"
                _isLoading.value = false
                println("❌ Waiter: Error en loadInitialData: ${e.message}")
            }
        }
    }

    private suspend fun loadTables() {
        try {
            firebaseTableRepository.getTables().collect { tablesList ->
                _tables.value = tablesList
                println("✅ Firebase: ${tablesList.size} mesas cargadas")
            }
        } catch (e: Exception) {
            println("❌ Waiter: Error cargando mesas: ${e.message}")
            throw e
        }
    }

    private suspend fun loadOrders() {
        try {
            firebaseOrderRepository.getActiveOrders().collect { firebaseOrders ->
                _orders.value = firebaseOrders
                println("✅ Firebase: ${firebaseOrders.size} órdenes activas cargadas")

                // DEBUG: Mostrar detalles de órdenes cargadas
                firebaseOrders.forEachIndexed { index, order ->
                    println("   Orden $index: Mesa ${order.tableNumber}, Estado: ${order.status}, Items: ${order.items.size}")
                }
            }
        } catch (e: Exception) {
            println("❌ Waiter: Error cargando órdenes: ${e.message}")
            throw e
        }
    }

    private suspend fun loadProducts() {
        try {
            firebaseProductRepository.getSellableProducts().collect { productsList ->
                _products.value = productsList
                println("✅ Waiter: ${productsList.size} productos cargados")
            }
        } catch (e: Exception) {
            println("❌ Waiter: Error cargando productos: ${e.message}")
            throw e
        }
    }

    // ==================== INICIALIZACIÓN FIREBASE ====================

    private fun initializeFirebase() {
        viewModelScope.launch {
            try {
                println("🔄 Waiter: Inicializando Firebase...")

                // Inicializar mesas por defecto en Firebase
                firebaseTableRepository.initializeDefaultTables()

                // Verificar conexión
                val testConnection = try {
                    firebaseTableRepository.getTables().first()
                    true
                } catch (e: Exception) {
                    false
                }

                _isFirebaseConnected.value = testConnection
                _connectionStatus.value = if (testConnection) "🟢 Conectado a cocina" else "🔴 Sin conexión"

                println("✅ Firebase: Inicialización completada - Conectado: $testConnection")

            } catch (e: Exception) {
                println("❌ Firebase: Error en inicialización: ${e.message}")
                _isFirebaseConnected.value = false
                _connectionStatus.value = "🔴 Error de conexión"
            }
        }
    }

    // ==================== GESTIÓN DE MESAS ====================

    fun setCurrentTable(tableId: Int) {
        val table = _tables.value.find { it.id == tableId }
        if (table != null) {
            _currentTableId.value = tableId
            _successMessage.value = "Mesa ${table.number} seleccionada"
            println("✅ Waiter: Mesa ${table.number} seleccionada")
        } else {
            _errorMessage.value = "Mesa no encontrada"
        }
    }

    // ==================== GESTIÓN DE PEDIDO ACTUAL ====================

    fun addItemToCurrentOrder(product: Product) {
        val existingItem = _currentOrderItems.value.find { it.productId == product.id }

        if (existingItem != null) {
            updateItemQuantity(product.id, existingItem.quantity + 1)
        } else {
            val newItem = OrderItem(
                product = product,
                quantity = 1
            )
            _currentOrderItems.value = _currentOrderItems.value + newItem
            _successMessage.value = "✅ ${product.name} agregado"
            println("✅ Waiter: ${product.name} agregado al pedido")
        }
    }

    fun updateItemQuantity(productId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeItemFromOrder(productId)
            return
        }

        _currentOrderItems.value = _currentOrderItems.value.map { item ->
            if (item.productId == productId) {
                val newSubtotal = newQuantity * item.unitPrice
                item.copy(
                    quantity = newQuantity,
                    subtotal = newSubtotal
                )
            } else item
        }
    }

    fun removeItemFromOrder(productId: String) {
        _currentOrderItems.value = _currentOrderItems.value.filterNot { it.productId == productId }
        _successMessage.value = "Producto removido"
    }

    fun clearCurrentOrder() {
        _currentOrderItems.value = emptyList()
        _currentTableId.value = null
        _successMessage.value = "Pedido limpiado"
        println("🔄 Waiter: Pedido actual limpiado")
    }

    // ==================== CREACIÓN DE ÓRDENES ====================

    fun createOrder(tableId: Int, tableNumber: Int, notes: String? = null) {
        viewModelScope.launch {
            try {
                val items = _currentOrderItems.value
                if (items.isEmpty()) {
                    _errorMessage.value = "El pedido no puede estar vacío"
                    println("❌ Waiter: Intento de crear orden vacía")
                    return@launch
                }

                val currentTime = System.currentTimeMillis()
                val total = items.sumOf { it.subtotal }

                println("📤 Waiter: CREANDO ORDEN PARA COCINA...")
                println("   - Mesa: $tableNumber")
                println("   - Items: ${items.size}")
                println("   - Total: S/. $total")

                items.forEach { item ->
                    println("     • ${item.quantity}x ${item.productName} - S/. ${item.subtotal}")
                }

                val order = Order(
                    id = UUID.randomUUID().toString(),
                    tableId = tableId,
                    tableNumber = tableNumber,
                    items = items,
                    status = OrderStatus.ENVIADO,
                    createdAt = currentTime,
                    updatedAt = currentTime,
                    total = total,
                    waiterId = "mesero_actual",
                    waiterName = "Mesero",
                    notes = notes
                )

                // ✅ SOLO Firebase - sin WebSocket
                firebaseOrderRepository.createOrder(order)
                firebaseTableRepository.assignOrderToTable(tableId, order.id)

                println("✅ Firebase: Orden ${order.id} enviada y mesa actualizada")

                // Notificar éxito
                _successMessage.value = "✅ Pedido enviado a cocina - Mesa $tableNumber"

                addNotification(
                    Notification(
                        type = NotificationType.ORDER_SENT,
                        title = "📤 Orden Enviada - Mesa $tableNumber",
                        message = "Pedido enviado a cocina con ${items.size} items",
                        orderId = order.id,
                        tableNumber = tableNumber
                    )
                )

                // Limpiar pedido actual
                clearCurrentOrder()

                println("✅ Waiter: Orden ${order.id} procesada exitosamente")

            } catch (e: Exception) {
                _errorMessage.value = "❌ Error al crear pedido: ${e.message}"
                println("❌ Waiter: Error en createOrder: ${e.message}")
            }
        }
    }

    // ==================== ACTUALIZACIÓN DE ESTADOS ====================

    fun markOrderAsServed(orderId: String) {
        viewModelScope.launch {
            try {
                println("🔄 Waiter: Marcando orden $orderId como servida")

                firebaseOrderRepository.updateOrderStatus(orderId, "COMPLETED")

                val order = _orders.value.find { it.id == orderId }
                order?.tableId?.let { tableId ->
                    firebaseTableRepository.clearTable(tableId)
                }

                _successMessage.value = "✅ Orden marcada como servida"

                // 🔥 NUEVO: Remover la orden de la lista local
                _orders.value = _orders.value.filter { it.id != orderId }

                println("✅ Waiter: Orden $orderId marcada como servida y removida")

            } catch (e: Exception) {
                _errorMessage.value = "❌ Error marcando orden como servida: ${e.message}"
                println("❌ Waiter: Error marcando orden como servida: ${e.message}")
            }
        }
    }

    // ==================== NOTIFICACIONES ====================

    private fun showStatusChangeNotification(previous: Order, current: Order) {
        val notification = when (current.status) {
            OrderStatus.ACEPTADO -> Notification(
                type = NotificationType.ORDER_ACCEPTED,
                title = "✅ Orden Aceptada - Mesa ${current.tableNumber}",
                message = "La cocina aceptó tu orden",
                orderId = current.id,
                tableNumber = current.tableNumber
            )
            OrderStatus.EN_PREPARACION -> Notification(
                type = NotificationType.ORDER_IN_PREPARATION,
                title = "👨‍🍳 Orden en Preparación - Mesa ${current.tableNumber}",
                message = "La cocina está preparando tu orden",
                orderId = current.id,
                tableNumber = current.tableNumber
            )
            OrderStatus.LISTO -> Notification(
                type = NotificationType.ORDER_READY,
                title = "🎉 ¡Orden Lista! - Mesa ${current.tableNumber}",
                message = "La orden está lista para servir",
                orderId = current.id,
                tableNumber = current.tableNumber
            )
            else -> null
        }

        notification?.let {
            addNotification(it)
            println("🔔 Waiter: Notificación enviada - ${it.title}")
        }
    }

    private fun addNotification(notification: Notification) {
        _notifications.value = listOf(notification) + _notifications.value.take(4)
        println("🔔 Waiter: Notificación agregada - ${notification.title}")
    }

    fun removeNotification(notification: Notification) {
        _notifications.value = _notifications.value.filter { it != notification }
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }

    // ==================== UTILIDADES ====================

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                loadTables()
                loadProducts()
                loadOrders()
                _successMessage.value = "✅ Datos actualizados"
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error actualizando datos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncWithFirebase() {
        viewModelScope.launch {
            try {
                println("🔄 Waiter: Sincronización manual con Firebase...")
                _isLoading.value = true

                initializeFirebase()
                loadTables()
                loadProducts()
                loadOrders()

                _isFirebaseConnected.value = true
                _connectionStatus.value = "🟢 Reconectado a cocina"
                _successMessage.value = "✅ Sincronización exitosa"

                println("✅ Waiter: Sincronización manual completada")

            } catch (e: Exception) {
                println("❌ Waiter: Error en syncWithFirebase: ${e.message}")
                _isFirebaseConnected.value = false
                _connectionStatus.value = "🔴 Error de sincronización"
                _errorMessage.value = "❌ Error sincronizando: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==================== PROPIEDADES COMPUTADAS ====================

    val currentOrderTotal: Double get() = _currentOrderItems.value.sumOf { it.subtotal }
    val currentOrderItemCount: Int get() = _currentOrderItems.value.sumOf { it.quantity }
    val occupiedTablesCount: Int get() = _tables.value.count { it.status == TableStatus.OCUPADA }
    val freeTablesCount: Int get() = _tables.value.count { it.status == TableStatus.LIBRE }
    val pendingOrdersCount: Int get() = _orders.value.count { it.status == OrderStatus.ENVIADO }
    val readyOrdersCount: Int get() = _orders.value.count { it.status == OrderStatus.LISTO }

    // Estado de conexión
    val combinedConnectionStatus: String get() =
        if (_isFirebaseConnected.value) "🟢 Conectado a cocina"
        else "🔴 Sin conexión"

    // ==================== MODELOS DE NOTIFICACIÓN ====================

    data class Notification(
        val id: String = System.currentTimeMillis().toString(),
        val type: NotificationType,
        val title: String,
        val message: String,
        val orderId: String? = null,
        val tableNumber: Int? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class NotificationType {
        ORDER_SENT,
        ORDER_ACCEPTED,
        ORDER_IN_PREPARATION,
        ORDER_READY,
        ORDER_CANCELLED
    }
}
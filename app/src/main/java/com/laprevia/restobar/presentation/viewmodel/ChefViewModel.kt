package com.laprevia.restobar.presentation.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laprevia.restobar.data.local.db.AppDatabase
import com.laprevia.restobar.data.local.sync.SyncManager
import com.laprevia.restobar.data.mapper.toDomain
import com.laprevia.restobar.data.mapper.toEntity
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.domain.repository.FirebaseInventoryRepository
import com.laprevia.restobar.domain.repository.FirebaseOrderRepository
import com.laprevia.restobar.domain.repository.FirebaseProductRepository  // ✅ AGREGAR IMPORT
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChefViewModel @Inject constructor(
    private val firebaseOrderRepository: FirebaseOrderRepository,
    private val firebaseInventoryRepository: FirebaseInventoryRepository,
    private val firebaseProductRepository: FirebaseProductRepository,  // ✅ AGREGADO
    private val db: AppDatabase,
    private val syncManager: SyncManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // StateFlows principales
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

    private val _isFirebaseConnected = MutableStateFlow(false)
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    private val _isInternetAvailable = MutableStateFlow(true)
    val isInternetAvailable: StateFlow<Boolean> = _isInternetAvailable.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _notifications = MutableStateFlow<List<ChefNotification>>(emptyList())
    val notifications: StateFlow<List<ChefNotification>> = _notifications.asStateFlow()

    private val _connectionMessage = MutableStateFlow<String?>(null)
    val connectionMessage: StateFlow<String?> = _connectionMessage.asStateFlow()

    private fun checkInternet(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun startNetworkMonitoring() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                timber.log.Timber.d("🌐 Chef: INTERNET DISPONIBLE")
                _isInternetAvailable.value = true
                viewModelScope.launch {
                    _connectionMessage.value = "🟢 Internet disponible - Sincronizando..."
                    _isFirebaseConnected.value = true
                    syncWithFirebase()
                    kotlinx.coroutines.delay(2000)
                    _connectionMessage.value = null
                }
            }

            override fun onLost(network: Network) {
                timber.log.Timber.d("📱 Chef: SIN INTERNET")
                _isInternetAvailable.value = false
                _isFirebaseConnected.value = false
                viewModelScope.launch {
                    _connectionMessage.value = "📱 SIN INTERNET - Modo offline"
                    _errorMessage.value = null
                    _successMessage.value = null
                    kotlinx.coroutines.delay(3000)
                    if (!_isInternetAvailable.value) {
                        _connectionMessage.value = null
                    }
                }
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (_isInternetAvailable.value != hasInternet) {
                    _isInternetAvailable.value = hasInternet
                    if (hasInternet) {
                        viewModelScope.launch { syncWithFirebase() }
                    }
                }
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        _isInternetAvailable.value = checkInternet()
        _isFirebaseConnected.value = _isInternetAvailable.value
    }

    init {
        timber.log.Timber.d("👨‍🍳 ChefViewModel INICIADO - Offline-First con Room + Firebase")
        startNetworkMonitoring()

        viewModelScope.launch {
            loadFromRoom()
            setupFirebaseRealtimeUpdates()
            if (_isInternetAvailable.value) {
                syncWithFirebase()
            } else {
                _connectionMessage.value = "📱 SIN INTERNET - Modo offline"
                _isFirebaseConnected.value = false
            }
        }
    }

    private suspend fun loadFromRoom() {
        try {
            val roomOrders = db.orderDao().getAll()
            val activeOrders = roomOrders.filter {
                it.status != "COMPLETED" && it.status != "CANCELLED"
            }
            // ✅ AGREGADO: Eliminar duplicados por ID
            val uniqueOrders = activeOrders.distinctBy { it.id }
            _orders.value = uniqueOrders.map { it.toDomain() }
            _isLoading.value = false
            timber.log.Timber.d("📱 Chef: ${_orders.value.size} órdenes cargadas desde Room (offline)")
            _orders.value.forEach { order ->
                timber.log.Timber.d("   - Mesa ${order.tableNumber}: ${order.status} - ${order.items.size} items")
            }
        } catch (e: Exception) {
            timber.log.Timber.d("❌ Chef: Error cargando desde Room: ${e.message}")
            _isLoading.value = false
        }
    }

    // ==================== FIREBASE REAL-TIME UPDATES ====================

    private fun setupFirebaseRealtimeUpdates() {
        // ✅ AGREGADO: Escuchar TODAS las órdenes al iniciar
        viewModelScope.launch {
            try {
                timber.log.Timber.d("🔥 Chef: Cargando TODAS las órdenes existentes de Firebase...")
                firebaseOrderRepository.getOrders().collect { allOrders ->
                    timber.log.Timber.d("📦 Chef: Recibidas ${allOrders.size} órdenes de Firebase")
                    allOrders.forEach { order ->
                        if (order.status != OrderStatus.COMPLETED && order.status != OrderStatus.CANCELLED) {
                            handleOrderFromFirebase(order)
                        }
                    }
                }
            } catch (e: Exception) {
                timber.log.Timber.d("❌ Chef: Error en getOrders: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                timber.log.Timber.d("🔥 Chef: Configurando Firebase Real-time Updates...")
                firebaseOrderRepository.listenToNewOrders().collect { newOrder ->
                    timber.log.Timber.d("🎯 Chef: ¡NUEVA ORDEN DEL MESERO! - Mesa ${newOrder.tableNumber}")
                    handleNewOrderFromFirebase(newOrder)
                }
            } catch (e: Exception) {
                timber.log.Timber.d("❌ Chef: Error en listenToNewOrders: ${e.message}")
            }
        }

        viewModelScope.launch {
            try {
                firebaseOrderRepository.listenToOrderChanges().collect { updatedOrder ->
                    timber.log.Timber.d("🔄 Chef: Orden actualizada desde Firebase - ${updatedOrder.id}")
                    handleUpdatedOrderFromFirebase(updatedOrder)
                }
            } catch (e: Exception) {
                timber.log.Timber.d("❌ Chef: Error en listenToOrderChanges: ${e.message}")
            }
        }

        // ✅ NUEVO: Escuchar específicamente cambios de ENTREGADO (para que el chef vea cuando el mesero entrega)
        viewModelScope.launch {
            try {
                firebaseOrderRepository.listenToOrderChanges().collect { updatedOrder ->
                    if (updatedOrder.status == OrderStatus.ENTREGADO) {
                        timber.log.Timber.d("🍽️ Chef: El MESERO entregó la comida - Mesa ${updatedOrder.tableNumber}")
                        db.orderDao().insert(updatedOrder.toEntity().copy(syncStatus = "SYNCED"))
                        refreshOrdersFromRoom()
                        addNotification(ChefNotification(
                            type = ChefNotificationType.ORDER_DELIVERED,
                            title = "🍽️ Comida Entregada - Mesa ${updatedOrder.tableNumber}",
                            message = "El mesero entregó la comida al cliente",
                            orderId = updatedOrder.id,
                            tableNumber = updatedOrder.tableNumber
                        ))
                    }
                }
            } catch (e: Exception) {
                timber.log.Timber.d("❌ Chef: Error escuchando ENTREGADO: ${e.message}")
            }
        }
    }

    // ✅ AGREGADO: Nuevo método para manejar órdenes de Firebase
    private suspend fun handleOrderFromFirebase(order: Order) {
        timber.log.Timber.d("📦 Chef: Procesando orden existente - Mesa ${order.tableNumber}")
        db.orderDao().insert(order.toEntity().copy(syncStatus = "SYNCED"))
        refreshOrdersFromRoom()
    }

    private suspend fun handleNewOrderFromFirebase(newOrder: Order) {
        timber.log.Timber.d("🎯 Chef: Procesando orden de Firebase - Mesa ${newOrder.tableNumber}")
        db.orderDao().insert(newOrder.toEntity().copy(syncStatus = "SYNCED"))
        refreshOrdersFromRoom()
        if (newOrder.status == OrderStatus.ENVIADO) {
            showNewOrderNotification(newOrder)
        }
    }

    private suspend fun handleUpdatedOrderFromFirebase(updatedOrder: Order) {
        timber.log.Timber.d("🔄 Chef: Procesando actualización - Mesa ${updatedOrder.tableNumber}")
        val previousOrder = _orders.value.find { it.id == updatedOrder.id }
        db.orderDao().insert(updatedOrder.toEntity().copy(syncStatus = "SYNCED"))
        refreshOrdersFromRoom()
        if (previousOrder != null && previousOrder.status != updatedOrder.status) {
            timber.log.Timber.d("🔔 Chef: Estado cambiado: ${previousOrder.status} -> ${updatedOrder.status}")
            showStatusChangeNotification(previousOrder, updatedOrder)
        }
        if (_selectedOrder.value?.id == updatedOrder.id) {
            _selectedOrder.value = updatedOrder
        }
    }

    private suspend fun refreshOrdersFromRoom() {
        val roomOrders = db.orderDao().getAll()
        // ✅ Incluir ENTREGADO (el cliente está comiendo) pero excluir COMPLETED y CANCELLED
        val activeOrders = roomOrders.filter {
            it.status != "COMPLETED" &&
                    it.status != "CANCELLED" &&
                    it.tableId != 0
        }.distinctBy { it.id }  // ✅ eliminar duplicados por ID

        _orders.value = activeOrders.mapNotNull { entity ->
            try {
                Order(
                    id          = entity.id,
                    tableId     = entity.tableId,
                    tableNumber = entity.tableNumber,
                    items = entity.toDomain().items,
                    status      = OrderStatus.valueOf(entity.status), // ENTREGADO ahora es válido
                    createdAt   = entity.createdAt,
                    updatedAt   = entity.updatedAt,
                    total       = entity.total,
                    waiterId    = entity.waiterId ?: "",
                    waiterName  = entity.waiterName ?: "",
                    notes       = entity.notes
                )
            } catch (e: Exception) {
                timber.log.Timber.d("❌ Chef: Error parseando orden ${entity.id}: ${e.message}")
                null
            }
        }
        timber.log.Timber.d("🗄️ Chef: Room → UI actualizado: ${_orders.value.size} órdenes activas")
        _orders.value.forEach { order ->
            timber.log.Timber.d("   - Mesa ${order.tableNumber}: ${order.status}")
        }
    }

    private fun syncWithFirebase() {
        viewModelScope.launch {
            try {
                timber.log.Timber.d("🔄 Chef: Sincronizando con Firebase...")
                _isFirebaseConnected.value = false
                syncManager.syncOrders()
                syncManager.downloadOrders()
                refreshOrdersFromRoom()
                _isFirebaseConnected.value = true
                timber.log.Timber.d("✅ Chef: Sincronización completada - ${_orders.value.size} órdenes activas")
            } catch (e: Exception) {
                timber.log.Timber.d("❌ Chef: Error en sync: ${e.message}")
                _isFirebaseConnected.value = false
                _errorMessage.value = "Error de conexión: ${e.message}"
            }
        }
    }

    fun manualSync() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (_isInternetAvailable.value) {
                    syncManager.syncOrders()
                    syncManager.downloadOrders()
                    refreshOrdersFromRoom()
                    _successMessage.value = "✅ Sincronización completada"
                    _isFirebaseConnected.value = true
                } else {
                    _errorMessage.value = "❌ Sin conexión a internet"
                    _isFirebaseConnected.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
                _isFirebaseConnected.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            try {
                timber.log.Timber.d("🔄 Chef: Actualizando orden $orderId a $status")
                val validChefStates = listOf(
                    OrderStatus.ACEPTADO, OrderStatus.EN_PREPARACION,
                    OrderStatus.LISTO, OrderStatus.COMPLETED, OrderStatus.CANCELLED
                )
                if (status !in validChefStates) {
                    timber.log.Timber.d("⚠️ Chef: Estado $status no permitido")
                    return@launch
                }

                val existingOrder = db.orderDao().getAll().find { it.id == orderId }
                if (existingOrder != null) {
                    val updatedEntity = existingOrder.copy(
                        status = status.name,
                        updatedAt = System.currentTimeMillis(),
                        syncStatus = "PENDING"
                    )
                    db.orderDao().insert(updatedEntity)
                    timber.log.Timber.d("💾 Orden actualizada en Room (PENDING) - $orderId")
                }

                if (_isInternetAvailable.value) {
                    try {
                        firebaseOrderRepository.updateOrderStatus(orderId, status.name)
                        db.orderDao().updateStatus(orderId, "SYNCED")
                        timber.log.Timber.d("✅ Orden actualizada en Firebase - $orderId")
                    } catch (e: Exception) {
                        timber.log.Timber.d("⚠️ Error en Firebase, se sincronizará después: ${e.message}")
                        _connectionMessage.value = "📱 Cambio guardado localmente - Se sincronizará después"
                        kotlinx.coroutines.delay(2000)
                        _connectionMessage.value = null
                    }
                } else {
                    timber.log.Timber.d("📱 Sin internet - Cambio guardado localmente")
                    _connectionMessage.value = "📱 SIN INTERNET - Cambio guardado localmente"
                    kotlinx.coroutines.delay(2000)
                    _connectionMessage.value = null
                }

                if (status == OrderStatus.EN_PREPARACION && _isInternetAvailable.value) {
                    updateInventoryForOrder(orderId)
                }

                refreshOrdersFromRoom()
                val order = _orders.value.find { it.id == orderId }
                _successMessage.value = when (status) {
                    OrderStatus.ACEPTADO -> "✅ Orden aceptada - Mesa ${order?.tableNumber}"
                    OrderStatus.EN_PREPARACION -> "👨‍🍳 Orden en preparación - Mesa ${order?.tableNumber}"
                    OrderStatus.LISTO -> "🎉 ¡Orden lista! - Mesa ${order?.tableNumber}"
                    OrderStatus.COMPLETED -> "✅ Orden completada - Mesa ${order?.tableNumber}"
                    OrderStatus.CANCELLED -> "❌ Orden cancelada - Mesa ${order?.tableNumber}"
                    else -> "Estado actualizado"
                }
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error actualizando orden: ${e.message}"
                timber.log.Timber.d("❌ Chef: Error: ${e.message}")
            }
        }
    }

    // ==================== GESTIÓN DE INVENTARIO ====================
    private suspend fun updateInventoryForOrder(orderId: String) {
        val order = _orders.value.find { it.id == orderId } ?: return
        timber.log.Timber.d("📦 Chef: Actualizando inventario para orden ${order.id}")
        order.items.forEach { item ->
            if (item.trackInventory) {
                try {
                    // ✅ USAR FirebaseProductRepository para obtener stock actual
                    val currentStock = firebaseProductRepository.getProductStock(item.productId)
                    val newStock = currentStock - item.quantity
                    if (newStock >= 0) {
                        firebaseProductRepository.updateProductStock(item.productId, newStock)
                        firebaseInventoryRepository.updateStock(item.productId, newStock)
                        timber.log.Timber.d("📦 Inventario: ${item.productName}: $currentStock → $newStock")
                        addNotification(ChefNotification(
                            type = ChefNotificationType.INVENTORY_UPDATED,
                            title = "📦 Inventario Actualizado",
                            message = "${item.productName}: ${currentStock.toInt()} → ${newStock.toInt()}",
                            orderId = order.id,
                            tableNumber = order.tableNumber
                        ))
                    } else {
                        timber.log.Timber.d("⚠️ Stock insuficiente para ${item.productName}")
                        _errorMessage.value = "Stock insuficiente: ${item.productName}"
                    }
                } catch (e: Exception) {
                    timber.log.Timber.d("❌ Error actualizando inventario: ${e.message}")
                }
            }
        }
    }

    // ==================== MÉTODOS DE CONVENIENCIA ====================
    fun acceptOrder(orderId: String) { updateOrderStatus(orderId, OrderStatus.ACEPTADO) }
    fun startOrderPreparation(orderId: String) { updateOrderStatus(orderId, OrderStatus.EN_PREPARACION) }
    fun markOrderAsReady(orderId: String) { updateOrderStatus(orderId, OrderStatus.LISTO) }
    fun completeOrder(orderId: String) { updateOrderStatus(orderId, OrderStatus.COMPLETED) }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            try {
                val order = _orders.value.find { it.id == orderId }
                if (order != null) {
                    val entity = db.orderDao().getAll().find { it.id == orderId }
                    entity?.let { db.orderDao().insert(it.copy(status = "CANCELLED", syncStatus = "PENDING")) }
                    if (_isInternetAvailable.value) {
                        try { firebaseOrderRepository.deleteOrder(orderId); db.orderDao().updateStatus(orderId, "SYNCED") }
                        catch (e: Exception) { timber.log.Timber.d("⚠️ Error en Firebase: ${e.message}") }
                    }
                    refreshOrdersFromRoom()
                    _successMessage.value = "Orden cancelada - Mesa ${order.tableNumber}"
                    if (_selectedOrder.value?.id == orderId) clearOrderSelection()
                }
            } catch (e: Exception) { _errorMessage.value = "Error cancelando orden: ${e.message}" }
        }
    }

    // ==================== NOTIFICACIONES ====================
    private fun showNewOrderNotification(order: Order) {
        addNotification(ChefNotification(
            type = ChefNotificationType.NEW_ORDER,
            title = "🆕 Nueva Orden - Mesa ${order.tableNumber}",
            message = "Pedido con ${order.items.size} items - Total: S/. ${"%.2f".format(order.total)}",
            orderId = order.id,
            tableNumber = order.tableNumber,
            itemsCount = order.items.size,
            totalAmount = order.total
        ))
    }

    private fun showStatusChangeNotification(previous: Order, current: Order) {
        val notification = when (current.status) {
            OrderStatus.ACEPTADO -> ChefNotification(
                type = ChefNotificationType.ORDER_ACCEPTED,
                title = "✅ Orden Aceptada - Mesa ${current.tableNumber}",
                message = "La orden ha sido aceptada",
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
            OrderStatus.ENTREGADO -> ChefNotification(
                type = ChefNotificationType.ORDER_DELIVERED,
                title = "🍽️ Comida Entregada - Mesa ${current.tableNumber}",
                message = "El mesero entregó la comida al cliente",
                orderId = current.id,
                tableNumber = current.tableNumber
            )
            else -> null
        }
        notification?.let { addNotification(it) }
    }

    private fun addNotification(notification: ChefNotification) {
        _notifications.value = listOf(notification) + _notifications.value.take(4)
    }

    fun removeNotification(notification: ChefNotification) { _notifications.value = _notifications.value.filter { it != notification } }
    fun clearAllNotifications() { _notifications.value = emptyList() }

    // ==================== UTILIDADES ====================
    fun selectOrder(order: Order) { _selectedOrder.value = order; timber.log.Timber.d("🎯 Chef: Orden seleccionada - Mesa ${order.tableNumber}") }
    fun clearOrderSelection() { _selectedOrder.value = null }
    fun refreshOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            if (_isInternetAvailable.value) syncWithFirebase()
            refreshOrdersFromRoom()
            _isLoading.value = false
        }
    }
    fun getOrderById(orderId: String): Order? = _orders.value.find { it.id == orderId }
    fun clearError() { _errorMessage.value = null }
    fun clearSuccessMessage() { _successMessage.value = null }
    fun clearConnectionMessage() { _connectionMessage.value = null }

    // ==================== PROPIEDADES COMPUTADAS ====================
    val newOrdersCount: Int get() = _orders.value.count { it.status == OrderStatus.ENVIADO }
    val acceptedOrdersCount: Int get() = _orders.value.count { it.status == OrderStatus.ACEPTADO }
    val inProgressOrdersCount: Int get() = _orders.value.count { it.status == OrderStatus.EN_PREPARACION }
    val readyOrdersCount: Int get() = _orders.value.count { it.status == OrderStatus.LISTO }
    val deliveredOrdersCount: Int get() = _orders.value.count { it.status == OrderStatus.ENTREGADO }
    val totalActiveOrders: Int get() = _orders.value.size

    val connectionStatus: String get() =
        if (!_isInternetAvailable.value) "🔴 SIN INTERNET - Modo offline"
        else if (_isFirebaseConnected.value) "🟢 Conectado a Firebase"
        else "🟡 Conectando..."

    fun getStatusText(status: OrderStatus): String = when (status) {
        OrderStatus.ENVIADO -> "🆕 Enviado por mesero"
        OrderStatus.ACEPTADO -> "✅ Aceptado"
        OrderStatus.EN_PREPARACION -> "👨‍🍳 En preparación"
        OrderStatus.LISTO -> "🎉 Listo para servir"
        OrderStatus.ENTREGADO -> "🍽️ Comida entregada"
        OrderStatus.COMPLETED -> "✅ Completado"
        OrderStatus.CANCELLED -> "❌ Cancelado"
        else -> status.name
    }

    // ==================== MODELO DE NOTIFICACIÓN ====================
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
        NEW_ORDER, ORDER_ACCEPTED, ORDER_IN_PREPARATION, ORDER_READY, ORDER_DELIVERED, ORDER_CANCELLED, INVENTORY_UPDATED
    }
}

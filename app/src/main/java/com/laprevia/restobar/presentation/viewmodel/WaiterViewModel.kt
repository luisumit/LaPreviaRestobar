package com.laprevia.restobar.presentation.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
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
import com.laprevia.restobar.domain.repository.FirebaseInventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.laprevia.restobar.data.local.db.AppDatabase
import com.laprevia.restobar.data.local.entity.OrderEntity
import com.laprevia.restobar.data.local.sync.SyncManager
import com.laprevia.restobar.data.mapper.toEntity
import com.laprevia.restobar.data.mapper.toDomain

@HiltViewModel
class WaiterViewModel @Inject constructor(
    private val firebaseTableRepository: FirebaseTableRepository,
    private val firebaseOrderRepository: FirebaseOrderRepository,
    private val firebaseProductRepository: FirebaseProductRepository,
    private val firebaseInventoryRepository: FirebaseInventoryRepository,
    private val db: AppDatabase,
    private val syncManager: SyncManager,
    @ApplicationContext private val context: Context
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

    private val _isFirebaseConnected = MutableStateFlow(false)
    val isFirebaseConnected: StateFlow<Boolean> = _isFirebaseConnected.asStateFlow()

    private val _connectionStatus = MutableStateFlow("Conectando...")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _isInternetAvailable = MutableStateFlow(true)
    val isInternetAvailable: StateFlow<Boolean> = _isInternetAvailable.asStateFlow()

    private val _connectionMessage = MutableStateFlow<String?>(null)
    val connectionMessage: StateFlow<String?> = _connectionMessage.asStateFlow()

    private val _currentOrderItems = MutableStateFlow<List<OrderItem>>(emptyList())
    val currentOrderItems: StateFlow<List<OrderItem>> = _currentOrderItems.asStateFlow()

    private val _currentTableId = MutableStateFlow<Int?>(null)
    val currentTableId: StateFlow<Int?> = _currentTableId.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

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
                println("🌐 Waiter: INTERNET DISPONIBLE")
                _isInternetAvailable.value = true
                viewModelScope.launch {
                    _connectionMessage.value = "🟢 Internet disponible - Sincronizando..."
                    _isFirebaseConnected.value = true
                    _connectionStatus.value = "🟢 Conectado a cocina"
                    syncPendingOrders()
                    syncWithFirebase()
                    kotlinx.coroutines.delay(2000)
                    _connectionMessage.value = null
                }
            }

            override fun onLost(network: Network) {
                println("📱 Waiter: SIN INTERNET")
                _isInternetAvailable.value = false
                _isFirebaseConnected.value = false
                viewModelScope.launch {
                    _connectionMessage.value = "📱 SIN INTERNET - Los pedidos se guardarán localmente"
                    _connectionStatus.value = "🔴 Sin conexión con cocina"
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
                        viewModelScope.launch {
                            syncPendingOrders()
                            syncWithFirebase()
                        }
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
        println("🟢 WaiterViewModel INICIADO - Offline-First + Firebase")
        startNetworkMonitoring()

        viewModelScope.launch {
            cleanOrdersWithTableZero()
            cleanCorruptOrders()
            cleanCorruptOrdersByTableId()
            initializeFirebase()
            delay(1000)
            loadInitialData()
            setupFirebaseRealtimeUpdates()
        }
    }

    private suspend fun cleanOrdersWithTableZero() {
        try {
            val ordersWithTableZero = db.orderDao().getAll().filter { it.tableId == 0 }
            if (ordersWithTableZero.isNotEmpty()) {
                println("🗑️ Eliminando ${ordersWithTableZero.size} órdenes con tableId=0")
                ordersWithTableZero.forEach { order ->
                    db.orderDao().deleteOrder(order.id)
                    println("   Eliminada orden corrupta: ${order.id}")
                }
                refreshOrdersFromRoom()
            }
        } catch (e: Exception) {
            println("❌ Error limpiando órdenes con tableId=0: ${e.message}")
        }
    }

    private suspend fun cleanCorruptOrders() {
        try {
            val allOrders = db.orderDao().getAll()
            val corruptOrders = allOrders.filter { it.tableId == 0 }
            if (corruptOrders.isNotEmpty()) {
                println("🗑️ Cancelando ${corruptOrders.size} órdenes corruptas con tableId=0")
                corruptOrders.forEach { order ->
                    db.orderDao().updateStatus(order.id, "CANCELLED")
                    println("   Cancelada orden corrupta: ${order.id}")
                }
                refreshOrdersFromRoom()
            }
        } catch (e: Exception) {
            println("❌ Error limpiando órdenes corruptas: ${e.message}")
        }
    }

    private suspend fun cleanCorruptOrdersByTableId() {
        try {
            val allOrders = db.orderDao().getAll()
            val corruptOrders = allOrders.filter { it.tableId == 0 }
            if (corruptOrders.isNotEmpty()) {
                println("🗑️ Eliminando ${corruptOrders.size} órdenes corruptas (tableId=0)")
                corruptOrders.forEach { order ->
                    db.orderDao().deleteOrder(order.id)
                    println("   Eliminada orden: ${order.id}")
                }
                refreshOrdersFromRoom()
            }
        } catch (e: Exception) {
            println("❌ Error limpiando órdenes corruptas por tableId: ${e.message}")
        }
    }

    private suspend fun syncPendingOrders() {
        try {
            val pendingOrders = db.orderDao().getPending()
            if (pendingOrders.isNotEmpty()) {
                println("🔄 Sincronizando ${pendingOrders.size} órdenes pendientes...")
                pendingOrders.forEach { orderEntity ->
                    try {
                        val order = orderEntity.toDomain()

                        firebaseOrderRepository.createOrder(order)
                        firebaseTableRepository.assignOrderToTable(order.tableId, order.id)

                        db.orderDao().updateStatus(order.id, "SYNCED")
                        println("✅ Orden sincronizada: ${order.id}")
                    } catch (e: Exception) {
                        println("❌ Error sincronizando orden: ${e.message}")
                    }
                }
                _successMessage.value = "✅ ${pendingOrders.size} pedido(s) sincronizados"
                refreshOrdersFromRoom()
            }
        } catch (e: Exception) {
            println("❌ Error en syncPendingOrders: ${e.message}")
        }
    }

    private fun setupFirebaseRealtimeUpdates() {
        viewModelScope.launch {
            try {
                println("🔥 Waiter: Configurando Firebase Real-time Updates...")
                firebaseOrderRepository.listenToOrderChanges().collect { updatedOrder ->
                    println("🔄 Waiter: Actualización del CHEF - Orden ${updatedOrder.id}")
                    println("   - Mesa: ${updatedOrder.tableNumber}")
                    println("   - Estado: ${updatedOrder.status}")
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
        viewModelScope.launch {
            try {
                println("🔄 Waiter: Guardando en Room - Mesa ${updatedOrder.tableNumber}, Estado: ${updatedOrder.status}")
                val previousOrder = _orders.value.find { it.id == updatedOrder.id }

                db.orderDao().insert(updatedOrder.toEntity())
                refreshOrdersFromRoom()

                if (previousOrder != null && previousOrder.status != updatedOrder.status) {
                    println("🔔 Waiter: El chef actualizó orden ${updatedOrder.id}")
                    showStatusChangeNotification(previousOrder, updatedOrder)
                    if (updatedOrder.status == OrderStatus.LISTO) {
                        _successMessage.value = "🎉 ¡Orden LISTA! - Mesa ${updatedOrder.tableNumber}"
                    }
                }
            } catch (e: Exception) {
                println("❌ Waiter: Error en handleUpdatedOrderFromFirebase: ${e.message}")
            }
        }
    }

    // ✅ CORREGIDO: Filtrar órdenes COMPLETED y CANCELLED
    private suspend fun refreshOrdersFromRoom() {
        val roomOrders = db.orderDao().getAll()
        val activeOrders = roomOrders.filter {
            it.status != "COMPLETED" && it.status != "CANCELLED"
        }
        _orders.value = activeOrders.map { it.toDomain() }
        println("🗄️ Room → _orders actualizado: ${_orders.value.size} órdenes activas")
    }

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
                println("✅ Waiter: Datos cargados - ${_tables.value.size} mesas, ${_orders.value.size} órdenes")
            } catch (e: Exception) {
                _errorMessage.value = "Error cargando datos: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    // ✅ CORREGIDO: Actualizar estado de mesas según órdenes activas
    private suspend fun loadTables() {
        try {
            val tablesList = firebaseTableRepository.getTables().first()
            val validTables = tablesList.filter { it.id in 1..8 }

            val activeOrderTableIds = _orders.value.map { it.tableId }.toSet()

            val updatedTables = validTables.map { table ->
                val hasActiveOrder = activeOrderTableIds.contains(table.id)
                if (hasActiveOrder && table.status != TableStatus.OCUPADA) {
                    table.copy(status = TableStatus.OCUPADA)
                } else if (!hasActiveOrder && table.status != TableStatus.LIBRE) {
                    table.copy(status = TableStatus.LIBRE, currentOrderId = null)
                } else {
                    table
                }
            }

            _tables.value = updatedTables
            println("✅ Mesas cargadas: ${updatedTables.size}")
        } catch (e: Exception) {
            println("❌ Waiter: Error cargando mesas: ${e.message}")
        }
    }

    private suspend fun loadOrders() {
        try {
            refreshOrdersFromRoom()
            println("✅ Room: ${_orders.value.size} órdenes activas")
        } catch (e: Exception) {
            println("❌ Waiter: Error cargando órdenes: ${e.message}")
        }
    }

    private suspend fun loadProducts() {
        try {
            firebaseProductRepository.getSellableProducts().collect { productsList ->
                _products.value = productsList.distinctBy { it.id }.sortedBy { it.name }
                println("✅ Waiter: ${_products.value.size} productos cargados")
            }
        } catch (e: Exception) {
            println("❌ Waiter: Error cargando productos: ${e.message}")
        }
    }

    private fun initializeFirebase() {
        viewModelScope.launch {
            try {
                println("🔄 Waiter: Inicializando Firebase...")
                firebaseTableRepository.initializeDefaultTables()
                val testConnection = try {
                    firebaseTableRepository.getTables().first()
                    true
                } catch (e: Exception) {
                    false
                }
                _isFirebaseConnected.value = testConnection && _isInternetAvailable.value
                _connectionStatus.value = if (_isFirebaseConnected.value) "🟢 Conectado a cocina" else "🔴 Sin conexión"
            } catch (e: Exception) {
                println("❌ Firebase: Error en inicialización: ${e.message}")
                _isFirebaseConnected.value = false
                _connectionStatus.value = "🔴 Error de conexión"
            }
        }
    }

    fun setCurrentTable(tableId: Int) {
        if (tableId !in 1..8) {
            _errorMessage.value = "Error: ID de mesa inválido ($tableId). Las mesas son del 1 al 8."
            return
        }

        val table = _tables.value.find { it.id == tableId }
        if (table != null) {
            _currentTableId.value = tableId
            _successMessage.value = "Mesa ${table.number} seleccionada"
            println("✅ Mesa seleccionada - ID: $tableId")
        } else {
            _errorMessage.value = "Mesa no encontrada"
        }
    }

    fun addItemToCurrentOrder(product: Product) {
        val existingItem = _currentOrderItems.value.find { it.productId == product.id }
        if (existingItem != null) {
            updateItemQuantity(product.id, existingItem.quantity + 1)
        } else {
            val newItem = OrderItem(
                productId = product.id,
                productName = product.name,
                productDescription = product.description,
                productCategory = product.category,
                quantity = 1,
                unitPrice = product.salePrice ?: 0.0,
                subtotal = product.salePrice ?: 0.0,
                trackInventory = product.trackInventory
            )
            _currentOrderItems.value = _currentOrderItems.value + newItem
            _successMessage.value = "✅ ${product.name} agregado"
        }
    }

    fun updateItemQuantity(productId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeItemFromOrder(productId)
            return
        }
        _currentOrderItems.value = _currentOrderItems.value.map { item ->
            if (item.productId == productId) {
                item.copy(quantity = newQuantity, subtotal = newQuantity * item.unitPrice)
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
    }

    fun createOrder(tableId: Int, tableNumber: Int, notes: String? = null) {
        viewModelScope.launch {
            try {
                if (tableId !in 1..8) {
                    _errorMessage.value = "Error: Mesa inválida (ID: $tableId)"
                    return@launch
                }

                val items = _currentOrderItems.value
                if (items.isEmpty()) {
                    _errorMessage.value = "El pedido no puede estar vacío"
                    return@launch
                }

                val currentTime = System.currentTimeMillis()
                val total = items.sumOf { it.subtotal }

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

                db.orderDao().insert(order.toEntity().copy(
                    syncStatus = if (_isInternetAvailable.value) "SYNCED" else "PENDING"
                ))

                if (_isInternetAvailable.value) {
                    try {
                        firebaseOrderRepository.createOrder(order)
                        firebaseTableRepository.assignOrderToTable(tableId, order.id)
                        db.orderDao().updateStatus(order.id, "SYNCED")
                        _successMessage.value = "✅ Pedido enviado a cocina - Mesa $tableNumber"
                    } catch (e: Exception) {
                        db.orderDao().updateStatus(order.id, "PENDING")
                        _successMessage.value = "📱 Pedido guardado localmente"
                    }
                } else {
                    _successMessage.value = "📱 SIN INTERNET - Pedido guardado localmente"
                }

                refreshOrdersFromRoom()
                clearCurrentOrder()
                loadTables()
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
            }
        }
    }

    fun markOrderAsDelivered(orderId: String) {
        viewModelScope.launch {
            try {
                println("🍽️ Waiter: Entregando comida - Orden $orderId")

                if (_isInternetAvailable.value) {
                    firebaseOrderRepository.updateOrderStatus(orderId, "ENTREGADO")
                    syncManager.syncOrders()
                }

                val entity = db.orderDao().getAll().find { it.id == orderId }
                entity?.let {
                    db.orderDao().insert(it.copy(
                        status = "ENTREGADO",
                        syncStatus = if (_isInternetAvailable.value) "SYNCED" else "PENDING",
                        updatedAt = System.currentTimeMillis()
                    ))
                }

                refreshOrdersFromRoom()
                _successMessage.value = "🍽️ Comida entregada - Mesa ${entity?.tableNumber}"
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
            }
        }
    }

    // ✅ CORREGIDO: Liberar mesa correctamente
    fun markTableAsFree(orderId: String) {
        viewModelScope.launch {
            try {
                println("🧹 Waiter: Liberando mesa - Orden $orderId")
                val order = _orders.value.find { it.id == orderId }
                order?.tableId?.let { tableId ->
                    firebaseTableRepository.clearTable(tableId)
                    firebaseOrderRepository.updateOrderStatus(orderId, "COMPLETED")

                    val entity = db.orderDao().getAll().find { it.id == orderId }
                    entity?.let {
                        db.orderDao().insert(it.copy(
                            status = "COMPLETED",
                            syncStatus = if (_isInternetAvailable.value) "SYNCED" else "PENDING"
                        ))
                    }

                    // Actualizar estado local
                    _orders.value = _orders.value.filter { it.id != orderId }

                    val currentTables = _tables.value.toMutableList()
                    val index = currentTables.indexOfFirst { it.id == tableId }
                    if (index != -1) {
                        currentTables[index] = currentTables[index].copy(
                            status = TableStatus.LIBRE,
                            currentOrderId = null
                        )
                        _tables.value = currentTables
                    }

                    _successMessage.value = "🧹 Mesa ${order.tableNumber} liberada"
                    println("✅ Mesa ${order.tableNumber} liberada")
                }
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
            }
        }
    }

    fun markOrderAsServed(orderId: String) {
        viewModelScope.launch {
            try {
                println("🔄 Waiter: Marcando orden $orderId como servida")
                firebaseOrderRepository.updateOrderStatus(orderId, "COMPLETED")
                val order = _orders.value.find { it.id == orderId }
                order?.tableId?.let { tableId ->
                    firebaseTableRepository.clearTable(tableId)
                    loadTables()

                    val currentTables = _tables.value.toMutableList()
                    val index = currentTables.indexOfFirst { it.id == tableId }
                    if (index != -1) {
                        currentTables[index] = currentTables[index].copy(
                            status = TableStatus.LIBRE,
                            currentOrderId = null
                        )
                        _tables.value = currentTables
                    }
                }

                val entity = db.orderDao().getAll().find { it.id == orderId }
                entity?.let {
                    db.orderDao().insert(it.copy(status = "COMPLETED", syncStatus = "SYNCED"))
                }

                _orders.value = _orders.value.filter { it.id != orderId }
                _successMessage.value = "✅ Orden marcada como servida"
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
            }
        }
    }

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
            OrderStatus.ENTREGADO -> Notification(
                type = NotificationType.ORDER_DELIVERED,
                title = "🍽️ Comida Entregada - Mesa ${current.tableNumber}",
                message = "La comida ha sido entregada al cliente",
                orderId = current.id,
                tableNumber = current.tableNumber
            )
            else -> null
        }
        notification?.let {
            addNotification(it)
        }
    }

    private fun addNotification(notification: Notification) {
        _notifications.value = listOf(notification) + _notifications.value.take(4)
    }

    fun removeNotification(notification: Notification) {
        _notifications.value = _notifications.value.filter { it != notification }
    }

    fun clearAllNotifications() {
        _notifications.value = emptyList()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun clearConnectionMessage() {
        _connectionMessage.value = null
    }

    // ✅ CORREGIDO: Refrescar datos en orden correcto
    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                refreshOrdersFromRoom()
                loadTables()
                loadProducts()
                _successMessage.value = "✅ Datos actualizados"
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun syncWithFirebase() {
        viewModelScope.launch {
            try {
                println("🔄 Waiter: Sincronización manual...")
                _isLoading.value = true
                if (_isInternetAvailable.value) {
                    syncPendingOrders()
                    initializeFirebase()
                    refreshData()
                    _isFirebaseConnected.value = true
                    _connectionStatus.value = "🟢 Conectado a cocina"
                    _successMessage.value = "✅ Sincronización exitosa"
                } else {
                    _errorMessage.value = "❌ Sin conexión a internet"
                }
            } catch (e: Exception) {
                _isFirebaseConnected.value = false
                _errorMessage.value = "❌ Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    val currentOrderTotal: Double get() = _currentOrderItems.value.sumOf { it.subtotal }
    val currentOrderItemCount: Int get() = _currentOrderItems.value.sumOf { it.quantity }
    val occupiedTablesCount: Int get() = _tables.value.count { it.status == TableStatus.OCUPADA }
    val freeTablesCount: Int get() = _tables.value.count { it.status == TableStatus.LIBRE }
    val pendingOrdersCount: Int get() = _orders.value.count { it.status == OrderStatus.ENVIADO }
    val readyOrdersCount: Int get() = _orders.value.count { it.status == OrderStatus.LISTO }

    val combinedConnectionStatus: String get() =
        if (!_isInternetAvailable.value) "🔴 SIN INTERNET - Modo offline"
        else if (_isFirebaseConnected.value) "🟢 Conectado a cocina"
        else "🔴 Sin conexión"

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
        ORDER_DELIVERED,
        ORDER_CANCELLED
    }
}
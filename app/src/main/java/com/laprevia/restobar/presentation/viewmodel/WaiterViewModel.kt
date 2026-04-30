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
        println("🟢 WaiterViewModel INICIADO - Offline-First + Firebase")
        startNetworkMonitoring()

        viewModelScope.launch {
            cleanCorruptOrders()
            cleanCorruptOrdersByTableId()
            initializeFirebase()
            delay(1000)
            loadInitialData()
            setupFirebaseRealtimeUpdates()
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

    private fun setupFirebaseRealtimeUpdates() {
        viewModelScope.launch {
            try {
                println("🔥 Waiter: Configurando Firebase Real-time Updates...")
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
                    println("   - Estado anterior: ${previousOrder.status}")
                    println("   - Estado nuevo: ${updatedOrder.status}")
                    println("   - Mesa: ${updatedOrder.tableNumber}")
                    showStatusChangeNotification(previousOrder, updatedOrder)
                    if (updatedOrder.status == OrderStatus.LISTO) {
                        println("🎉 Waiter: ¡Orden LISTA para mesa ${updatedOrder.tableNumber}!")
                        _successMessage.value = "🎉 ¡Orden LISTA! - Mesa ${updatedOrder.tableNumber}"
                    }
                } else if (previousOrder == null) {
                    println("➕ Waiter: Orden nueva guardada en Room - Mesa ${updatedOrder.tableNumber}")
                }
                println("📊 Waiter: Estado actual - ${_orders.value.size} órdenes:")
                _orders.value.forEach { order ->
                    println("   - Mesa ${order.tableNumber}: ${order.status} (${order.items.size} items)")
                }
            } catch (e: Exception) {
                println("❌ Waiter: Error en handleUpdatedOrderFromFirebase: ${e.message}")
            }
        }
    }

    // ✅ CORREGIDO: Ahora NO filtra ENTREGADO
    private suspend fun refreshOrdersFromRoom() {
        val roomOrders = db.orderDao().getAll()
        // Solo filtrar COMPLETED (ENTREGADO debe mostrarse para poder liberar mesa)
        val activeOrders = roomOrders.filter { it.status != "COMPLETED" }
        _orders.value = activeOrders.map { it.toDomain() }
        println("🗄️ Room → _orders actualizado: ${_orders.value.size} órdenes activas")
        _orders.value.forEach { order ->
            println("   - Mesa ${order.tableNumber}: ${order.status} (${order.items.size} items)")
        }
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
            val tablesList = firebaseTableRepository.getTables().first()
            _tables.value = tablesList
            println("✅ Firebase: ${tablesList.size} mesas cargadas")
            tablesList.forEach { table ->
                println("   - Mesa ID: ${table.id}, Número: ${table.number}, Estado: ${table.status}")
            }
        } catch (e: Exception) {
            println("❌ Waiter: Error cargando mesas: ${e.message}")
        }
    }

    private suspend fun loadOrders() {
        try {
            refreshOrdersFromRoom()
            println("✅ Room: Órdenes cargadas inicialmente - ${_orders.value.size} activas")
            _orders.value.forEachIndexed { index, order ->
                println("   Orden $index: Mesa ${order.tableNumber}, Estado: ${order.status}, Items: ${order.items.size}")
            }
        } catch (e: Exception) {
            println("❌ Waiter: Error cargando órdenes desde Room: ${e.message}")
        }
    }

    private suspend fun loadProducts() {
        try {
            firebaseProductRepository.getSellableProducts().collect { productsList ->
                val uniqueProducts = productsList.distinctBy { it.id }.sortedBy { it.name }
                _products.value = uniqueProducts
                println("✅ Waiter: ${uniqueProducts.size} productos únicos cargados")
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
                println("✅ Firebase: Inicialización completada - Conectado: $testConnection")
            } catch (e: Exception) {
                println("❌ Firebase: Error en inicialización: ${e.message}")
                _isFirebaseConnected.value = false
                _connectionStatus.value = "🔴 Error de conexión"
            }
        }
    }

    fun setCurrentTable(tableId: Int) {
        println("🔍 Buscando mesa con ID: $tableId")
        println("Mesas disponibles: ${_tables.value.map { "ID:${it.id} Num:${it.number}" }}")
        val table = _tables.value.find { it.id == tableId }
        if (table != null) {
            _currentTableId.value = tableId
            _successMessage.value = "Mesa ${table.number} seleccionada"
            println("✅ Mesa seleccionada - ID: $tableId, Número: ${table.number}")
        } else {
            _errorMessage.value = "Mesa no encontrada"
            println("❌ Mesa con ID $tableId no encontrada")
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
            println("✅ Waiter: ${product.name} agregado al pedido")
            println("📦 Items en carrito: ${_currentOrderItems.value.size}")
            _currentOrderItems.value.forEach { item ->
                println("   - ${item.productName} x${item.quantity}")
            }
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
                item.copy(quantity = newQuantity, subtotal = newSubtotal)
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

    fun createOrder(tableId: Int, tableNumber: Int, notes: String? = null) {
        viewModelScope.launch {
            try {
                if (tableId == 0) {
                    _errorMessage.value = "Error: Primero selecciona una mesa"
                    println("❌ Waiter: Intento de crear orden con tableId=0")
                    return@launch
                }
                val items = _currentOrderItems.value
                if (items.isEmpty()) {
                    _errorMessage.value = "El pedido no puede estar vacío"
                    println("❌ Waiter: Intento de crear orden vacía")
                    return@launch
                }
                val currentTime = System.currentTimeMillis()
                val total = items.sumOf { it.subtotal }
                println("📤 Waiter: CREANDO ORDEN...")
                println("   - tableId: $tableId, tableNumber: $tableNumber")
                println("   - Items: ${items.size}")
                println("   - Total: S/. $total")
                items.forEach { item ->
                    println("     • ${item.quantity}x ${item.productName}")
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
                println("✅ Orden creada - ID: ${order.id}, Items: ${order.items.size}")

                db.orderDao().insert(order.toEntity())
                println("💾 Orden guardada en Room con items")

                if (_isInternetAvailable.value) {
                    try {
                        firebaseOrderRepository.createOrder(order)
                        db.orderDao().updateStatus(order.id, "SYNCED")
                        firebaseTableRepository.assignOrderToTable(tableId, order.id)
                        println("✅ Orden subida a Firebase - Mesa $tableNumber")
                        _successMessage.value = "✅ Pedido enviado a cocina - Mesa $tableNumber"
                    } catch (e: Exception) {
                        println("⚠️ Error en Firebase: ${e.message}")
                        _successMessage.value = "📱 Pedido guardado localmente"
                    }
                } else {
                    println("📱 Sin internet - Orden guardada localmente")
                    _successMessage.value = "📱 Pedido guardado localmente"
                }
                refreshOrdersFromRoom()
                clearCurrentOrder()
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error al crear pedido: ${e.message}"
                println("❌ Error: ${e.message}")
            }
        }
    }

    fun markOrderAsDelivered(orderId: String) {
        viewModelScope.launch {
            try {
                println("🍽️ Waiter: Entregando comida - Orden $orderId")
                firebaseOrderRepository.updateOrderStatus(orderId, "ENTREGADO")
                val entity = db.orderDao().getAll().find { it.id == orderId }
                entity?.let {
                    db.orderDao().insert(it.copy(status = "ENTREGADO", syncStatus = "SYNCED"))
                }
                refreshOrdersFromRoom()
                _successMessage.value = "🍽️ Comida entregada - Mesa ${entity?.tableNumber}"
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
                println("❌ Waiter: Error entregando comida: ${e.message}")
            }
        }
    }

    fun markTableAsFree(orderId: String) {
        viewModelScope.launch {
            try {
                println("🧹 Waiter: Liberando mesa - Orden $orderId")
                val order = _orders.value.find { it.id == orderId }
                order?.tableId?.let { tableId ->
                    firebaseTableRepository.clearTable(tableId)
                    println("✅ Mesa $tableId marcada como LIBRE en Firebase")
                    firebaseOrderRepository.updateOrderStatus(orderId, "COMPLETED")
                    val entity = db.orderDao().getAll().find { it.id == orderId }
                    entity?.let {
                        db.orderDao().insert(it.copy(status = "COMPLETED", syncStatus = "SYNCED"))
                    }
                    delay(300)
                    loadTables()
                    refreshOrdersFromRoom()
                    _successMessage.value = "🧹 Mesa ${order.tableNumber} liberada"
                    println("✅ Mesa ${order.tableNumber} ahora está LIBRE")
                }
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error: ${e.message}"
                println("❌ Waiter: Error liberando mesa: ${e.message}")
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
                    println("✅ Mesa $tableId marcada como LIBRE en Firebase")
                    delay(300)
                    loadTables()
                    val currentTables = _tables.value.toMutableList()
                    val index = currentTables.indexOfFirst { it.id == tableId }
                    if (index != -1) {
                        val updatedTable = currentTables[index].copy(
                            status = TableStatus.LIBRE,
                            currentOrderId = null
                        )
                        currentTables[index] = updatedTable
                        _tables.value = currentTables
                        println("✅ Mesa ${updatedTable.number} actualizada a LIBRE en UI")
                    }
                }
                val entity = db.orderDao().getAll().find { it.id == orderId }
                if (entity != null) {
                    db.orderDao().insert(entity.copy(status = "COMPLETED", syncStatus = "SYNCED"))
                }
                _successMessage.value = "✅ Orden marcada como servida - Mesa ${order?.tableNumber}"
                refreshOrdersFromRoom()
                println("✅ Waiter: Orden $orderId marcada como servida y mesa desocupada")
            } catch (e: Exception) {
                _errorMessage.value = "❌ Error marcando orden como servida: ${e.message}"
                println("❌ Waiter: Error marcando orden como servida: ${e.message}")
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

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun clearConnectionMessage() {
        _connectionMessage.value = null
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
                if (_isInternetAvailable.value) {
                    initializeFirebase()
                    loadTables()
                    loadProducts()
                    loadOrders()
                    _isFirebaseConnected.value = true
                    _connectionStatus.value = "🟢 Reconectado a cocina"
                    _successMessage.value = "✅ Sincronización exitosa"
                } else {
                    _errorMessage.value = "❌ Sin conexión a internet"
                    _connectionMessage.value = "📱 Modo offline activo"
                }
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
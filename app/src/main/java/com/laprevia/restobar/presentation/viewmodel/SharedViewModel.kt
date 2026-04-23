// presentation/viewmodel/SharedViewModel.kt - VERSIÓN CORREGIDA
package com.laprevia.restobar.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laprevia.restobar.data.remote.api.ApiService
import com.laprevia.restobar.data.remote.websocket.RealTimeWebSocketClient
import com.laprevia.restobar.data.remote.websocket.WebSocketEvent
import com.laprevia.restobar.data.remote.websocket.WebSocketMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val webSocketClient: RealTimeWebSocketClient,
    private val apiService: ApiService
) : ViewModel() {

    private val _connectionStatus = MutableStateFlow<WebSocketEvent>(WebSocketEvent.Connecting)
    val connectionStatus: StateFlow<WebSocketEvent> = _connectionStatus.asStateFlow()

    private val _httpConnectionStatus = MutableStateFlow<Boolean?>(null)
    val httpConnectionStatus: StateFlow<Boolean?> = _httpConnectionStatus.asStateFlow()

    // Eventos para los ViewModels específicos
    private val _orderUpdates = MutableSharedFlow<OrderUpdateEvent>()
    val orderUpdates: SharedFlow<OrderUpdateEvent> = _orderUpdates.asSharedFlow()

    private val _tableUpdates = MutableSharedFlow<TableUpdateEvent>()
    val tableUpdates: SharedFlow<TableUpdateEvent> = _tableUpdates.asSharedFlow()

    private val _productUpdates = MutableSharedFlow<ProductUpdateEvent>()
    val productUpdates: SharedFlow<ProductUpdateEvent> = _productUpdates.asSharedFlow()

    private val _notifications = MutableSharedFlow<NotificationEvent>()
    val notifications: SharedFlow<NotificationEvent> = _notifications.asSharedFlow()

    // 🔥 NUEVO: Variables para controlar notificaciones duplicadas
    private var lastConnectionState: Boolean? = null
    private var lastConnectionNotificationTime: Long = 0
    private var checkCount = 0

    init {
        Timber.d("🚀 SharedViewModel INICIADO")
        setupWebSocket()
        connectWebSocket()
        checkConnectionRepeatedly()

        viewModelScope.launch {
            delay(3000)
            testHttpConnectionQuietly() // 🔥 CAMBIADO: Usar versión silenciosa
        }
    }

    private fun setupWebSocket() {
        webSocketClient.webSocketEvents
            .onEach { event ->
                _connectionStatus.value = event
                handleWebSocketEvent(event)
            }
            .launchIn(viewModelScope)
    }

    fun connectWebSocket() {
        viewModelScope.launch {
            try {
                Timber.d("🎯 ANDROID: Intentando conectar a WebSocket...")
                Timber.d("📍 URL: ${webSocketClient.getCurrentUrl()}")
                webSocketClient.connect()
                Timber.d("🔄 Llamada a connect() completada")
            } catch (e: Exception) {
                Timber.e("❌❌❌ ERROR CRÍTICO conectando WebSocket: ${e.message}")
            }
        }
    }

    // 🔥 CORREGIDO: Test HTTP normal (solo para uso manual)
    fun testHttpConnection() {
        viewModelScope.launch {
            try {
                Timber.d("🌐 TEST HTTP MANUAL: Probando conexión con el servidor...")
                _httpConnectionStatus.value = null

                val response = apiService.healthCheck()

                if (response.isSuccessful) {
                    val healthResponse = response.body()
                    Timber.d("✅✅✅ HTTP CONECTADO: ${healthResponse?.message ?: "Servidor respondiendo"}")
                    _httpConnectionStatus.value = true

                    // Solo enviar notificación si fue exitoso (manual)
                    _notifications.emit(
                        NotificationEvent(
                            title = "✅ Servidor Conectado",
                            body = "Conexión HTTP establecida correctamente",
                            type = "HTTP_SUCCESS",
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } else {
                    Timber.e("❌ HTTP ERROR: ${response.code()} - ${response.message()}")
                    _httpConnectionStatus.value = false
                    // 🔥 NO enviar notificación automática de error HTTP
                }
            } catch (e: Exception) {
                Timber.e("❌❌❌ HTTP FAILED: ${e.message}")
                _httpConnectionStatus.value = false
                // 🔥 NO enviar notificación automática de error
            }
        }
    }

    // 🔥 NUEVO: Test HTTP sin notificaciones (para verificaciones automáticas)
    private fun testHttpConnectionQuietly() {
        viewModelScope.launch {
            try {
                val response = apiService.healthCheck()
                if (response.isSuccessful) {
                    _httpConnectionStatus.value = true
                    Timber.d("✅ HTTP CONECTADO (silencioso)")
                } else {
                    _httpConnectionStatus.value = false
                    Timber.d("❌ HTTP ERROR (silencioso): ${response.code()}")
                }
            } catch (e: Exception) {
                _httpConnectionStatus.value = false
                Timber.d("❌ HTTP FAILED (silencioso): ${e.message}")
            }
        }
    }

    // 🔥 CORREGIDO: Verificación periódica sin spam de notificaciones
    private fun checkConnectionRepeatedly() {
        viewModelScope.launch {
            delay(2000)

            while (true) {
                delay(5000) // Cada 5 segundos
                checkCount++

                val isConnected = webSocketClient.isConnected()
                val currentUrl = webSocketClient.getCurrentUrl()

                // 🔥 NUEVO: Solo loggear si el estado cambió
                if (isConnected != lastConnectionState) {
                    Timber.d("🔍 CHECK $checkCount - WS: $isConnected - URL: $currentUrl")
                    lastConnectionState = isConnected
                }

                // 🔥 MODIFICADO: Probar HTTP solo cuando sea necesario (cada 30 segundos)
                if (checkCount % 6 == 0) {
                    testHttpConnectionQuietly()
                }

                if (!isConnected && checkCount > 2) {
                    Timber.w("⚠️ WebSocket desconectado - Intentando reconexión...")
                    delay(2000)
                    connectWebSocket()
                }
            }
        }
    }

    // 🔥 MEJORADO: Debug más completo
    fun debugConnection() {
        viewModelScope.launch {
            Timber.d("🎯 DEBUG COMPLETO INICIADO")
            Timber.d("📍 WebSocket URL: ${webSocketClient.getCurrentUrl()}")
            Timber.d("📊 Estado WS: ${webSocketClient.getConnectionStatus()}")
            Timber.d("🔗 WS Conectado: ${webSocketClient.isConnected()}")
            Timber.d("🔄 WS Conectando: ${webSocketClient.isConnecting()}")

            // Probar HTTP también
            testHttpConnection() // 🔥 Usar versión manual para debug

            // Forzar reconexión WebSocket
            connectWebSocket()

            // Verificar después de 3 segundos
            delay(3000)
            Timber.d("🔍 DEBUG POST-CONEXIÓN:")
            Timber.d("   - WS Conectado: ${webSocketClient.isConnected()}")
            Timber.d("   - HTTP Conectado: ${_httpConnectionStatus.value}")

            // Enviar mensaje de prueba si está conectado
            if (webSocketClient.isConnected()) {
                sendTestMessage()
            }
        }
    }

    // 🔥 NUEVO: Enviar mensaje de prueba
    private fun sendTestMessage() {
        viewModelScope.launch {
            try {
                val testMessage = WebSocketMessage.Notification(
                    title = "Test Debug",
                    body = "Mensaje de prueba desde debug - ${System.currentTimeMillis()}",
                    notificationType = "DEBUG",
                    targetUserId = null
                )
                webSocketClient.sendMessage(testMessage)
                Timber.d("📤 Mensaje de prueba enviado")
            } catch (e: Exception) {
                Timber.e("❌ Error enviando mensaje de prueba: ${e.message}")
            }
        }
    }

    fun disconnectWebSocket() {
        webSocketClient.disconnect()
        Timber.d("🔌 WebSocket desconectado manualmente")
    }

    // 🔥 COMPATIBLE: Métodos para enviar mensajes (sin cambios)
    fun sendOrderStatusUpdate(orderId: String, newStatus: String, tableId: String? = null, tableNumber: Int? = null) {
        viewModelScope.launch {
            try {
                val message = WebSocketMessage.OrderStatusChanged(
                    orderId = orderId,
                    newStatus = newStatus,
                    tableId = tableId,
                    tableNumber = tableNumber
                )
                webSocketClient.sendMessage(message)
                Timber.d("📤 Enviado update de orden: $orderId -> $newStatus")
            } catch (e: Exception) {
                Timber.e("❌ Error enviando update de orden: ${e.message}")
            }
        }
    }

    fun sendTableStatusUpdate(tableId: String, newStatus: String, orderId: String? = null) {
        viewModelScope.launch {
            try {
                val message = WebSocketMessage.TableStatusChanged(
                    tableId = tableId,
                    newStatus = newStatus,
                    orderId = orderId
                )
                webSocketClient.sendMessage(message)
                Timber.d("📤 Enviado update de mesa: $tableId -> $newStatus")
            } catch (e: Exception) {
                Timber.e("❌ Error enviando update de mesa: ${e.message}")
            }
        }
    }

    fun sendProductUpdate(productId: String, action: String, productName: String? = null, updateType: String? = null) {
        viewModelScope.launch {
            try {
                val message = WebSocketMessage.ProductUpdated(
                    productId = productId,
                    action = action,
                    productName = productName,
                    updateType = updateType
                )
                webSocketClient.sendMessage(message)
                Timber.d("📤 Enviado update de producto: $productId -> $action")
            } catch (e: Exception) {
                Timber.e("❌ Error enviando update de producto: ${e.message}")
            }
        }
    }

    fun sendNotification(title: String, body: String, notificationType: String, targetUserId: String? = null) {
        viewModelScope.launch {
            try {
                val message = WebSocketMessage.Notification(
                    title = title,
                    body = body,
                    notificationType = notificationType,
                    targetUserId = targetUserId
                )
                webSocketClient.sendMessage(message)
                Timber.d("📤 Enviada notificación: $title")
            } catch (e: Exception) {
                Timber.e("❌ Error enviando notificación: ${e.message}")
            }
        }
    }

    private fun handleWebSocketEvent(event: WebSocketEvent) {
        Timber.d("🎯 WebSocket Event: $event")

        when (event) {
            is WebSocketEvent.MessageReceived -> {
                handleWebSocketMessage(event.message)
            }
            is WebSocketEvent.Error -> {
                Timber.e("❌ WebSocket error: ${event.error}")
                // 🔥 MODIFICADO: No probar HTTP automáticamente en cada error
                if (event.error?.contains("failed to connect") == true) {
                    viewModelScope.launch {
                        delay(2000)
                        testHttpConnectionQuietly()
                    }
                }
            }
            is WebSocketEvent.Connected -> {
                Timber.d("✅✅✅ WebSocket CONECTADO - Comunicación en tiempo real activa")
                broadcastConnectionStatus(true)
            }
            is WebSocketEvent.Disconnected -> {
                Timber.d("🔌 WebSocket DESCONECTADO")
                broadcastConnectionStatus(false)
            }
            is WebSocketEvent.Connecting -> {
                Timber.d("🔄 WebSocket CONECTANDO...")
            }
        }
    }

    private fun handleWebSocketMessage(message: WebSocketMessage) {
        when (message) {
            is WebSocketMessage.OrderStatusChanged -> {
                Timber.d("🛎️ WebSocket: Orden ${message.orderId} -> ${message.newStatus}")
                viewModelScope.launch {
                    _orderUpdates.emit(
                        OrderUpdateEvent(
                            orderId = message.orderId,
                            newStatus = message.newStatus,
                            tableId = message.tableId,
                            tableNumber = message.tableNumber,
                            timestamp = message.timestamp
                        )
                    )
                }
            }
            is WebSocketMessage.TableStatusChanged -> {
                Timber.d("🪑 WebSocket: Mesa ${message.tableId} -> ${message.newStatus}")
                viewModelScope.launch {
                    _tableUpdates.emit(
                        TableUpdateEvent(
                            tableId = message.tableId,
                            newStatus = message.newStatus,
                            orderId = message.orderId,
                            timestamp = message.timestamp
                        )
                    )
                }
            }
            is WebSocketMessage.ProductUpdated -> {
                Timber.d("📦 WebSocket: Producto ${message.productId} -> ${message.action}")
                viewModelScope.launch {
                    _productUpdates.emit(
                        ProductUpdateEvent(
                            productId = message.productId,
                            action = message.action,
                            updateType = message.updateType,
                            productName = message.productName,
                            timestamp = message.timestamp
                        )
                    )
                }
            }
            is WebSocketMessage.Notification -> {
                Timber.d("🔔 WebSocket: Notificación - ${message.title}")
                viewModelScope.launch {
                    _notifications.emit(
                        NotificationEvent(
                            title = message.title,
                            body = message.body,
                            type = message.notificationType,
                            targetUserId = message.targetUserId,
                            timestamp = message.timestamp
                        )
                    )
                }
            }
        }
    }

    // 🔥 CORREGIDO: Broadcast connection status sin duplicados
    private fun broadcastConnectionStatus(isConnected: Boolean) {
        viewModelScope.launch {
            val statusMessage = if (isConnected) {
                "🟢 CONECTADO - Comunicación en tiempo real activa"
            } else {
                "🔴 DESCONECTADO - Modo local activado"
            }
            Timber.d(statusMessage)

            // 🔥 NUEVO: Evitar notificaciones duplicadas de conexión
            val currentTime = System.currentTimeMillis()
            val timeSinceLastNotification = currentTime - lastConnectionNotificationTime
            val isDuplicate = timeSinceLastNotification < 30000 // 30 segundos

            if (!isDuplicate) {
                _notifications.emit(
                    NotificationEvent(
                        title = "Estado de Conexión",
                        body = statusMessage,
                        type = "CONNECTION_STATUS",
                        timestamp = currentTime
                    )
                )
                lastConnectionNotificationTime = currentTime
                Timber.d("📤 Notificación de conexión enviada")
            } else {
                Timber.d("⏭️  Notificación de conexión omitida (duplicada)")
            }
        }
    }

    // 🔥 NUEVO: Métodos de utilidad para HTTP
    fun getHttpStatusText(): String {
        return when (_httpConnectionStatus.value) {
            true -> "🟢 HTTP CONECTADO"
            false -> "🔴 HTTP ERROR"
            null -> "⚪ HTTP NO PROBADO"
        }
    }

    fun isHttpConnected(): Boolean {
        return _httpConnectionStatus.value == true
    }

    // Métodos de utilidad existentes
    fun isConnected(): Boolean {
        return webSocketClient.isConnected()
    }

    fun getConnectionStatusText(): String {
        return webSocketClient.getConnectionStatus()
    }

    // 🔥 NUEVO: Obtener info completa de conexión
    fun getFullConnectionInfo(): String {
        return """
            📊 ESTADO DE CONEXIÓN:
            🔗 WebSocket: ${getConnectionStatusText()}
            🌐 HTTP: ${getHttpStatusText()}
            📍 URL: ${webSocketClient.getCurrentUrl()}
        """.trimIndent()
    }
}


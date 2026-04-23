// data/remote/websocket/RealTimeWebSocketClient.kt - VERSIÓN CORREGIDA
package com.laprevia.restobar.data.remote.websocket

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow // 🔥 NUEVA IMPORTACIÓN

@Singleton
class RealTimeWebSocketClient @Inject constructor(
    private val context: Context,
    private val webSocketUrl: String
) {
    private val _webSocketEvents = MutableStateFlow<WebSocketEvent>(WebSocketEvent.Connecting)
    val webSocketEvents: StateFlow<WebSocketEvent> = _webSocketEvents.asStateFlow()

    private var webSocket: WebSocket? = null
    private val gson = Gson()

    // 🔥 MEJORADO: Configuración más robusta de OkHttpClient
    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    // 🔥 NUEVO: URLs de fallback para mayor robustez
    private val fallbackUrls = listOf(
        webSocketUrl, // URL principal inyectada
        "ws://10.0.2.2:8080/ws",    // Fallback emulador
        "ws://192.168.0.104:8080/ws", // Fallback dispositivo físico
        "ws://localhost:8080/ws"     // Fallback localhost
    )

    private var currentUrlIndex = 0
    private var retryCount = 0
    private val maxRetries = 5
    private var isManualDisconnect = false

    init {
        Timber.d("🚀 RealTimeWebSocketClient INICIADO")
        Timber.d("📍 URL WebSocket: $webSocketUrl")
        Timber.d("📱 Context: ${context.packageName}")
    }

    fun connect() {
        // 🔥 NUEVO: Verificar conectividad de red primero
        if (!isNetworkAvailable()) {
            Timber.e("❌ No hay conexión de red disponible")
            _webSocketEvents.value = WebSocketEvent.Error("Sin conexión de red")
            attemptReconnection()
            return
        }

        try {
            val currentUrl = fallbackUrls[currentUrlIndex]
            Timber.d("🔄 Conectando WebSocket a: $currentUrl (Intento ${retryCount + 1}/$maxRetries)")
            _webSocketEvents.value = WebSocketEvent.Connecting

            val request = Request.Builder()
                .url(currentUrl)
                .addHeader("User-Agent", "Android-App-LaPrevia")
                .addHeader("Origin", "http://localhost")
                .addHeader("Connection", "Upgrade")
                .addHeader("Upgrade", "websocket")
                .build()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Timber.d("✅✅✅ WebSocket CONECTADO exitosamente a: $currentUrl")
                    retryCount = 0 // Resetear contador en éxito
                    currentUrlIndex = 0 // Volver a URL principal
                    _webSocketEvents.value = WebSocketEvent.Connected

                    // 🔥 NUEVO: Enviar mensaje de identificación
                    sendIdentificationMessage()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Timber.d("📨 Mensaje WebSocket recibido: $text")
                    try {
                        val message = parseMessage(text)
                        if (message != null) {
                            _webSocketEvents.value = WebSocketEvent.MessageReceived(message)
                        } else {
                            Timber.w("⚠️ Mensaje WebSocket no reconocido: $text")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "❌ Error parseando mensaje WebSocket")
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Timber.d("🔌 WebSocket cerrándose: $code - $reason")
                    _webSocketEvents.value = WebSocketEvent.Disconnected
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Timber.d("🔌 WebSocket cerrado: $code - $reason")
                    _webSocketEvents.value = WebSocketEvent.Disconnected

                    // 🔥 NUEVO: Reconectar solo si no fue desconexión manual
                    if (!isManualDisconnect) {
                        attemptReconnection()
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Timber.e(t, "❌ FALLA WebSocket - URL: $currentUrl")
                    Timber.e("🔧 Detalles: ${t.message}")
                    Timber.e("📡 Response: ${response?.code} - ${response?.message}")

                    _webSocketEvents.value = WebSocketEvent.Error(t.message ?: "Connection failed")

                    // 🔥 MEJORADO: Lógica de reconexión inteligente
                    if (retryCount < maxRetries) {
                        retryCount++
                        attemptReconnection()
                    } else {
                        // Cambiar a siguiente URL
                        currentUrlIndex = (currentUrlIndex + 1) % fallbackUrls.size
                        retryCount = 0
                        Timber.d("🔄 Cambiando a URL alternativa: ${fallbackUrls[currentUrlIndex]}")
                        attemptReconnection()
                    }
                }
            })
        } catch (e: Exception) {
            Timber.e(e, "❌ Error en conexión WebSocket")
            _webSocketEvents.value = WebSocketEvent.Error(e.message ?: "Connection error")
            attemptReconnection()
        }
    }

    fun disconnect() {
        Timber.d("🔌 Desconectando WebSocket manualmente")
        isManualDisconnect = true
        webSocket?.close(1000, "Manual disconnect")
        webSocket = null
        _webSocketEvents.value = WebSocketEvent.Disconnected
    }

    fun sendMessage(message: WebSocketMessage) {
        try {
            if (webSocket == null) {
                Timber.w("⚠️ WebSocket no conectado, intentando reconectar...")
                isManualDisconnect = false
                connect()
                // Pequeño delay para permitir la conexión
                Thread.sleep(1000)
            }

            val jsonMessage = gson.toJson(message)
            val isSent = webSocket?.send(jsonMessage)

            if (isSent == true) {
                Timber.d("📤 Mensaje WebSocket enviado: ${message::class.simpleName}")
            } else {
                Timber.e("❌ Error: No se pudo enviar mensaje WebSocket")
                attemptReconnection()
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error enviando mensaje WebSocket")
            attemptReconnection()
        }
    }

    // 🔥 NUEVO: Verificar disponibilidad de red
    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo
            val isAvailable = networkInfo != null && networkInfo.isConnected
            Timber.d("📡 Estado de red: ${if (isAvailable) "🟢 CONECTADA" else "🔴 SIN CONEXIÓN"}")
            isAvailable
        } catch (e: Exception) {
            Timber.e("❌ Error verificando conectividad: ${e.message}")
            false
        }
    }

    // 🔥 NUEVO: Mensaje de identificación al conectar
    private fun sendIdentificationMessage() {
        try {
            val identificationMessage = WebSocketMessage.Notification(
                title = "Android App Connected",
                body = "Dispositivo Android conectado - ${Build.MANUFACTURER} ${Build.MODEL}",
                notificationType = "CONNECTION",
                targetUserId = null
            )
            val jsonMessage = gson.toJson(identificationMessage)
            webSocket?.send(jsonMessage)
            Timber.d("📤 Mensaje de identificación enviado")
        } catch (e: Exception) {
            Timber.e("❌ Error enviando identificación: ${e.message}")
        }
    }

    // 🔥 CORREGIDO: Reconexión con backoff exponencial (sin errores de tipos)
    private fun attemptReconnection() {
        if (isManualDisconnect) {
            Timber.d("🔌 Reconexión cancelada (desconexión manual)")
            return
        }

        // 🔥 CORRECCIÓN: Sin errores de tipos
        val baseDelay = 1000L // Usar Long directamente
        val exponentialDelay = when {
            retryCount == 0 -> baseDelay
            retryCount == 1 -> baseDelay * 2
            retryCount == 2 -> baseDelay * 4
            retryCount == 3 -> baseDelay * 8
            retryCount == 4 -> baseDelay * 16
            else -> 30000L // Máximo 30 segundos
        }

        val delay = minOf(exponentialDelay, 30000L) // Máximo 30 segundos

        Timber.d("🔄 Reconexión en ${delay}ms... (Retry $retryCount)")

        Thread {
            Thread.sleep(delay)
            if (_webSocketEvents.value !is WebSocketEvent.Connected && !isManualDisconnect) {
                Timber.d("🔄 Ejecutando reconexión automática...")
                connect()
            }
        }.start()
    }

    private fun parseMessage(json: String): WebSocketMessage? {
        return try {
            val typeMap = gson.fromJson(json, Map::class.java)
            val type = typeMap["type"] as? String

            when (type) {
                "ORDER_STATUS_CHANGED" -> {
                    gson.fromJson(json, WebSocketMessage.OrderStatusChanged::class.java)
                }
                "TABLE_STATUS_CHANGED" -> {
                    gson.fromJson(json, WebSocketMessage.TableStatusChanged::class.java)
                }
                "PRODUCT_UPDATED" -> {
                    gson.fromJson(json, WebSocketMessage.ProductUpdated::class.java)
                }
                "NOTIFICATION" -> {
                    gson.fromJson(json, WebSocketMessage.Notification::class.java)
                }
                "CONNECTED" -> {
                    WebSocketMessage.Notification(
                        title = "Conectado al servidor",
                        body = typeMap["message"] as? String ?: "Conexión establecida",
                        notificationType = "SYSTEM",
                        timestamp = typeMap["timestamp"] as? Long ?: System.currentTimeMillis()
                    )
                }
                else -> {
                    when {
                        json.contains("orderId") && json.contains("newStatus") -> {
                            gson.fromJson(json, WebSocketMessage.OrderStatusChanged::class.java)
                        }
                        json.contains("tableId") && json.contains("newStatus") -> {
                            gson.fromJson(json, WebSocketMessage.TableStatusChanged::class.java)
                        }
                        json.contains("productId") && json.contains("action") -> {
                            gson.fromJson(json, WebSocketMessage.ProductUpdated::class.java)
                        }
                        json.contains("title") && json.contains("body") -> {
                            gson.fromJson(json, WebSocketMessage.Notification::class.java)
                        }
                        else -> {
                            Timber.w("🔍 Mensaje no reconocido: $json")
                            null
                        }
                    }
                }
            }
        } catch (e: JsonSyntaxException) {
            Timber.e("❌ Error de sintaxis JSON: ${e.message}")
            null
        } catch (e: Exception) {
            Timber.e("❌ Error parseando mensaje: ${e.message}")
            null
        }
    }

    fun isConnected(): Boolean {
        return _webSocketEvents.value == WebSocketEvent.Connected
    }

    fun getConnectionStatus(): String {
        return when (_webSocketEvents.value) {
            is WebSocketEvent.Connected -> "🟢 Conectado"
            is WebSocketEvent.Connecting -> "🔄 Conectando..."
            is WebSocketEvent.Disconnected -> "🔴 Desconectado"
            is WebSocketEvent.Error -> "❌ Error: ${(_webSocketEvents.value as WebSocketEvent.Error).error}"
            else -> "⚪ Desconocido"
        }
    }

    // 🔥 NUEVO: Método para obtener URL actual
    fun getCurrentUrl(): String {
        return fallbackUrls[currentUrlIndex]
    }

    // 🔥 NUEVO: Método para verificar si está intentando conectar
    fun isConnecting(): Boolean {
        return _webSocketEvents.value is WebSocketEvent.Connecting
    }

    // 🔥 NUEVO: Reiniciar conexión completamente
    fun restartConnection() {
        Timber.d("🔄 Reiniciando conexión WebSocket...")
        disconnect()
        Thread.sleep(1000)
        isManualDisconnect = false
        retryCount = 0
        currentUrlIndex = 0
        connect()
    }

    // 🔥 NUEVO: Método para debug
    fun debugInfo(): String {
        return """
            📊 WebSocket Debug Info:
            📍 URL Actual: ${getCurrentUrl()}
            🔗 Estado: ${getConnectionStatus()}
            🔄 Conectando: ${isConnecting()}
            ✅ Conectado: ${isConnected()}
            🔁 Reintentos: $retryCount/$maxRetries
            📡 Red Disponible: ${isNetworkAvailable()}
        """.trimIndent()
    }
}
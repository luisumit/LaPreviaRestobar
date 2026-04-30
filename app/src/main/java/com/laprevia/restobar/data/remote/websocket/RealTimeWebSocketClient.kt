package com.laprevia.restobar.data.remote.websocket

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import kotlin.math.pow

@Singleton
class RealTimeWebSocketClient @Inject constructor(
    private val context: Context,
    private val webSocketUrl: String
) {
    private val _webSocketEvents = MutableStateFlow<WebSocketEvent>(WebSocketEvent.Connecting)
    val webSocketEvents: StateFlow<WebSocketEvent> = _webSocketEvents.asStateFlow()

    private var webSocket: WebSocket? = null
    private val gson = Gson()

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private val fallbackUrls = listOf(
        webSocketUrl,
        "ws://10.0.2.2:8080/ws",
        "ws://192.168.0.104:8080/ws",
        "ws://localhost:8080/ws"
    )

    private var currentUrlIndex = 0
    private var retryCount = 0
    private val maxRetries = 5
    private var isManualDisconnect = false

    init {
        Timber.d("🚀 RealTimeWebSocketClient INICIADO")
    }

    fun connect() {
        if (!isNetworkAvailable()) {
            Timber.e("❌ No hay conexión de red disponible")
            _webSocketEvents.value = WebSocketEvent.Error("Sin conexión de red")
            attemptReconnection()
            return
        }

        try {
            val currentUrl = fallbackUrls[currentUrlIndex]
            _webSocketEvents.value = WebSocketEvent.Connecting

            val request = Request.Builder()
                .url(currentUrl)
                .addHeader("User-Agent", "Android-App-LaPrevia")
                .build()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Timber.i("✅ WebSocket CONECTADO a: %s", currentUrl)
                    retryCount = 0
                    currentUrlIndex = 0
                    _webSocketEvents.value = WebSocketEvent.Connected
                    sendIdentificationMessage()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val message = parseMessage(text)
                        if (message != null) {
                            _webSocketEvents.value = WebSocketEvent.MessageReceived(message)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "❌ Error parseando mensaje WebSocket")
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    _webSocketEvents.value = WebSocketEvent.Disconnected
                    if (!isManualDisconnect) attemptReconnection()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Timber.e(t, "❌ FALLA WebSocket")
                    _webSocketEvents.value = WebSocketEvent.Error(t.message ?: "Connection failed")
                    if (retryCount < maxRetries) {
                        retryCount++
                        attemptReconnection()
                    } else {
                        currentUrlIndex = (currentUrlIndex + 1) % fallbackUrls.size
                        retryCount = 0
                        attemptReconnection()
                    }
                }
            })
        } catch (e: Exception) {
            Timber.e(e, "❌ Error en conexión WebSocket")
            attemptReconnection()
        }
    }

    fun disconnect() {
        isManualDisconnect = true
        webSocket?.close(1000, "Manual disconnect")
        webSocket = null
        _webSocketEvents.value = WebSocketEvent.Disconnected
    }

    fun sendMessage(message: WebSocketMessage) {
        try {
            if (webSocket == null) {
                isManualDisconnect = false
                connect()
            }
            val jsonMessage = gson.toJson(message)
            webSocket?.send(jsonMessage)
        } catch (e: Exception) {
            Timber.e(e, "❌ Error enviando mensaje WebSocket")
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private fun sendIdentificationMessage() {
        try {
            val identificationMessage = WebSocketMessage.Notification(
                title = "Android App Connected",
                body = "Dispositivo Android conectado - ${Build.MANUFACTURER} ${Build.MODEL}",
                notificationType = "CONNECTION",
                targetUserId = null
            )
            webSocket?.send(gson.toJson(identificationMessage))
        } catch (e: Exception) {
            Timber.e(e, "❌ Error enviando identificación")
        }
    }

    private fun attemptReconnection() {
        if (isManualDisconnect) return
        val delay = minOf(1000L * 2.toDouble().pow(retryCount).toLong(), 30000L)
        
        Thread {
            Thread.sleep(delay)
            if (_webSocketEvents.value !is WebSocketEvent.Connected && !isManualDisconnect) {
                connect()
            }
        }.start()
    }

    private fun parseMessage(json: String): WebSocketMessage? {
        return try {
            val typeMap = gson.fromJson(json, Map::class.java)
            val type = typeMap["type"] as? String

            when (type) {
                "ORDER_STATUS_CHANGED" -> gson.fromJson(json, WebSocketMessage.OrderStatusChanged::class.java)
                "TABLE_STATUS_CHANGED" -> gson.fromJson(json, WebSocketMessage.TableStatusChanged::class.java)
                "PRODUCT_UPDATED" -> gson.fromJson(json, WebSocketMessage.ProductUpdated::class.java)
                "NOTIFICATION" -> gson.fromJson(json, WebSocketMessage.Notification::class.java)
                else -> null
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error parseando mensaje")
            null
        }
    }

    fun isConnected(): Boolean = _webSocketEvents.value == WebSocketEvent.Connected

    fun isConnecting(): Boolean = _webSocketEvents.value == WebSocketEvent.Connecting

    fun getCurrentUrl(): String = if (currentUrlIndex < fallbackUrls.size) fallbackUrls[currentUrlIndex] else "Desconocida"

    fun getConnectionStatus(): String {
        return when (_webSocketEvents.value) {
            is WebSocketEvent.Connected -> "CONECTADO"
            is WebSocketEvent.Connecting -> "CONECTANDO"
            is WebSocketEvent.Disconnected -> "DESCONECTADO"
            is WebSocketEvent.Error -> "ERROR"
            else -> "DESCONOCIDO"
        }
    }
}
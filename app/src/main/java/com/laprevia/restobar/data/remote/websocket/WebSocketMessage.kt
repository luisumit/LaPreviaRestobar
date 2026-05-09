// data/remote/websocket/WebSocketMessage.kt - ACTUALIZADO
package com.laprevia.restobar.data.remote.websocket

import com.google.gson.annotations.SerializedName

sealed class WebSocketMessage {
    data class OrderStatusChanged(
        @SerializedName("type") val type: String = "ORDER_STATUS_CHANGED",
        @SerializedName("orderId") val orderId: String,
        @SerializedName("newStatus") val newStatus: String,
        @SerializedName("tableId") val tableId: String? = null,
        @SerializedName("tableNumber") val tableNumber: Int? = null,
        @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
    ) : WebSocketMessage()

    data class TableStatusChanged(
        @SerializedName("type") val type: String = "TABLE_STATUS_CHANGED",
        @SerializedName("tableId") val tableId: String,
        @SerializedName("newStatus") val newStatus: String,
        @SerializedName("orderId") val orderId: String? = null,
        @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
    ) : WebSocketMessage()

    data class ProductUpdated(
        @SerializedName("type") val type: String = "PRODUCT_UPDATED",
        @SerializedName("productId") val productId: String,
        @SerializedName("action") val action: String,
        @SerializedName("productName") val productName: String? = null,
        @SerializedName("updateType") val updateType: String? = null,
        @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
    ) : WebSocketMessage()

    data class Notification(
        @SerializedName("type") val type: String = "NOTIFICATION",
        @SerializedName("title") val title: String,
        @SerializedName("body") val body: String,
        @SerializedName("notificationType") val notificationType: String,
        @SerializedName("targetUserId") val targetUserId: String? = null,
        @SerializedName("timestamp") val timestamp: Long = System.currentTimeMillis()
    ) : WebSocketMessage()
}

sealed class WebSocketEvent {
    object Connected : WebSocketEvent()
    data class MessageReceived(val message: WebSocketMessage) : WebSocketEvent()
    data class Error(val error: String) : WebSocketEvent()
    object Disconnected : WebSocketEvent()
    object Connecting : WebSocketEvent()
}

// Events.kt (VERSIÓN CORREGIDA)
package com.laprevia.restobar.presentation.viewmodel

/**
 * Archivo centralizado para eventos de comunicación entre ViewModels
 */

data class OrderUpdateEvent(
    val orderId: String,
    val newStatus: String,
    val tableId: String? = null,
    val tableNumber: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class NotificationEvent(
    val title: String,
    val body: String,
    val type: String,
    val targetUserId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// 🔥 CORREGIDO: Compatible con SharedViewModel
data class TableUpdateEvent(
    val tableId: String,              // ✅ Cambiado de Int a String
    val newStatus: String,            // ✅ Solo newStatus (sin oldStatus)
    val orderId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ProductUpdateEvent(
    val productId: String,
    val action: String,
    val updateType: String? = null,
    val productName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// Tipos de notificación predefinidos
object NotificationTypes {
    const val NEW_ORDER = "NEW_ORDER"
    const val ORDER_ACCEPTED = "ORDER_ACCEPTED"
    const val ORDER_IN_PREPARATION = "ORDER_IN_PREPARATION"
    const val ORDER_READY = "ORDER_READY"
    const val ORDER_CANCELLED = "ORDER_CANCELLED"
    const val SYSTEM_ALERT = "SYSTEM_ALERT"
    const val CONNECTION_STATUS = "CONNECTION_STATUS"
}
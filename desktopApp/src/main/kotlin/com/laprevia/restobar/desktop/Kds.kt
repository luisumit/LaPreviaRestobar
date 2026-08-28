package com.laprevia.restobar.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.domain.repository.FirebaseOrderRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * KDS (Kitchen Display System): pantalla de cocina en la PC.
 * Pedidos en grande, orden FIFO, cronometro de espera y un boton por tarjeta
 * para avanzar el estado — el mismo flujo del chef en el celular.
 */

// Estados que ve la cocina y su siguiente accion
private fun nextAction(status: OrderStatus): Pair<String, OrderStatus>? = when (status) {
    OrderStatus.PENDING, OrderStatus.ENVIADO -> "ACEPTAR" to OrderStatus.ACEPTADO
    OrderStatus.ACEPTADO -> "PREPARAR" to OrderStatus.EN_PREPARACION
    OrderStatus.EN_PREPARACION -> "LISTO ✓" to OrderStatus.LISTO
    else -> null
}

private fun statusColor(status: OrderStatus): Color = when (status) {
    OrderStatus.PENDING, OrderStatus.ENVIADO -> Color(0xFFFF6E40)   // nuevo: coral
    OrderStatus.ACEPTADO -> Color(0xFFFFB74D)                        // aceptado: naranja
    OrderStatus.EN_PREPARACION -> Color(0xFFFFB300)                  // en fuego: ambar
    OrderStatus.LISTO -> Color(0xFF66BB6A)                           // listo: verde
    else -> Color(0xFF8E8E99)
}

private fun statusLabel(status: OrderStatus): String = when (status) {
    OrderStatus.PENDING, OrderStatus.ENVIADO -> "NUEVO"
    OrderStatus.ACEPTADO -> "ACEPTADO"
    OrderStatus.EN_PREPARACION -> "EN PREPARACION"
    OrderStatus.LISTO -> "LISTO - esperando mozo"
    else -> status.name
}

@Composable
fun KdsView(orders: List<Order>, orderRepo: FirebaseOrderRepository) {
    val scope = rememberCoroutineScope()

    // Reloj para el cronometro de espera (se refresca cada 15 s)
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            now = System.currentTimeMillis()
        }
    }

    val kitchenOrders = orders
        .filter {
            it.status in setOf(
                OrderStatus.PENDING, OrderStatus.ENVIADO, OrderStatus.ACEPTADO,
                OrderStatus.EN_PREPARACION, OrderStatus.LISTO
            )
        }
        .sortedBy { it.createdAt } // FIFO: el mas antiguo primero

    if (kitchenOrders.isEmpty()) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("🍳", fontSize = 44.sp)
            Text("Cocina al dia — sin pedidos pendientes", color = Color(0xFFF5F5F5).copy(alpha = 0.6f), fontSize = 16.sp)
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(300.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(kitchenOrders, key = { it.id }) { order ->
            val accent = statusColor(order.status)
            val minutes = ((now - order.createdAt) / 60000).coerceAtLeast(0)
            var updating by remember(order.id) { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    // Cabecera: mesa grande + cronometro
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "M" + order.tableNumber.toString().padStart(2, '0'),
                            fontWeight = FontWeight.Bold, fontSize = 30.sp, color = Color(0xFFF5F5F5)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "hace $minutes min",
                                color = if (minutes >= 20) Color(0xFFFF5252) else Color(0xFFF5F5F5).copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                fontWeight = if (minutes >= 20) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    // Chip de estado
                    Box(
                        Modifier.background(accent.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(statusLabel(order.status), color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    // Items en grande (lo que la cocina necesita leer de lejos)
                    order.items.forEach { item ->
                        Text(
                            "${item.quantity} × ${item.productName}",
                            color = Color(0xFFF5F5F5), fontSize = 17.sp, fontWeight = FontWeight.Medium
                        )
                    }
                    if (!order.notes.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text("⚠ ${order.notes}", color = Color(0xFFFFB74D), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    // Boton de siguiente estado
                    nextAction(order.status)?.let { (label, next) ->
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (updating) return@Button
                                updating = true
                                scope.launch {
                                    runCatching { orderRepo.updateOrderStatus(order.id, next.name) }
                                    updating = false
                                }
                            },
                            enabled = !updating,
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF12121A))
                        }
                    }
                }
            }
        }
    }
}

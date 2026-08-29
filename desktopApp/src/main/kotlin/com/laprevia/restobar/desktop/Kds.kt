package com.laprevia.restobar.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    OrderStatus.EN_PREPARACION -> "MARCAR LISTO" to OrderStatus.LISTO
    else -> null
}

// Colores semanticos de estado — compartidos con la vista Pedidos
internal fun kdsStatusColor(status: OrderStatus): Color = when (status) {
    OrderStatus.PENDING, OrderStatus.ENVIADO -> Lp.Coral      // nuevo: coral
    OrderStatus.ACEPTADO -> Lp.Warn                            // aceptado: naranja
    OrderStatus.EN_PREPARACION -> Lp.Amber                     // en fuego: ambar
    OrderStatus.LISTO, OrderStatus.ENTREGADO -> Lp.Green       // listo/entregado: verde
    else -> Lp.TextDim
}

internal fun kdsStatusLabel(status: OrderStatus): String = when (status) {
    OrderStatus.PENDING, OrderStatus.ENVIADO -> "NUEVO"
    OrderStatus.ACEPTADO -> "ACEPTADO"
    OrderStatus.EN_PREPARACION -> "EN PREPARACIÓN"
    OrderStatus.LISTO -> "LISTO"
    OrderStatus.ENTREGADO -> "ENTREGADO"
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
            Icon(Icons.Default.Restaurant, contentDescription = null, tint = Lp.TextMuted, modifier = Modifier.size(46.dp))
            Spacer(Modifier.height(12.dp))
            Text("Cocina al día — sin pedidos pendientes", color = Lp.TextDim, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        return
    }

    val nuevos = kitchenOrders.count { it.status == OrderStatus.PENDING || it.status == OrderStatus.ENVIADO }
    val enFuego = kitchenOrders.count { it.status == OrderStatus.ACEPTADO || it.status == OrderStatus.EN_PREPARACION }
    val listos = kitchenOrders.count { it.status == OrderStatus.LISTO }

    Column {
        // Semaforo de carga: el chef ve su cola de un vistazo
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatChip("$nuevos", "NUEVOS", Lp.Coral)
            StatChip("$enFuego", "EN PREPARACIÓN", Lp.Amber)
            StatChip("$listos", "LISTOS", Lp.Green)
        }
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(320.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(kitchenOrders, key = { it.id }) { order ->
                val accent = kdsStatusColor(order.status)
                val minutes = ((now - order.createdAt) / 60000).coerceAtLeast(0)
                val warning = minutes in 12..19
                val late = minutes >= 20
                var updating by remember(order.id) { mutableStateOf(false) }

                // Borde de la tarjeta: rojo si esta atrasado, verde si esta listo
                val borderColor = when {
                    late -> Lp.Red.copy(alpha = 0.35f)
                    order.status == OrderStatus.LISTO -> Lp.Green.copy(alpha = 0.35f)
                    else -> null
                }

                Column(
                    Modifier.lpCard(18.dp, borderTint = borderColor).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cabecera: mesa en grande + cronometro con escalon de urgencia
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                        Column {
                            Text(
                                tableLabel(order.tableNumber), fontFamily = BebasFamily,
                                fontSize = 50.sp, lineHeight = 46.sp, color = Lp.Text
                            )
                            order.waiterName?.takeIf { it.isNotBlank() }?.let {
                                Text(it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Lp.TextDim)
                            }
                        }
                        Box(
                            Modifier.background(
                                when {
                                    late -> Lp.Red.copy(alpha = 0.16f)
                                    warning -> Lp.Warn.copy(alpha = 0.16f)
                                    else -> Color.White.copy(alpha = 0.07f)
                                },
                                RoundedCornerShape(9.dp)
                            ).padding(horizontal = 11.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "$minutes MIN", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                                style = TabularNumbers,
                                color = when {
                                    late -> Lp.Red
                                    warning -> Lp.Warn
                                    else -> Lp.TextSoft
                                }
                            )
                        }
                    }
                    StatusPill(kdsStatusLabel(order.status), accent)
                    // Items en grande (lo que la cocina necesita leer de lejos)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        order.items.forEach { item ->
                            Text(
                                "${item.quantity} × ${item.productName}",
                                color = Lp.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (!order.notes.isNullOrBlank()) {
                        Box(Modifier.background(Lp.Warn.copy(alpha = 0.12f), RoundedCornerShape(9.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("Nota: ${order.notes}", color = Lp.Warn, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    // Accion: siguiente estado, o espera del mozo si ya esta listo
                    val action = nextAction(order.status)
                    if (action != null) {
                        val (label, next) = action
                        Box(
                            Modifier.fillMaxWidth().height(50.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(if (updating) accent.copy(alpha = 0.4f) else accent)
                                .lpHover(0.10f, enabled = !updating)
                                .clickable(enabled = !updating) {
                                    updating = true
                                    scope.launch {
                                        runCatching { orderRepo.updateOrderStatus(order.id, next.name) }
                                        updating = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = 1.5.sp, color = Lp.OnAccent)
                        }
                    } else if (order.status == OrderStatus.LISTO) {
                        Box(
                            Modifier.fillMaxWidth().height(50.dp)
                                .border(1.5.dp, Lp.Green.copy(alpha = 0.5f), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ESPERANDO AL MOZO", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp, color = Lp.Green)
                        }
                    }
                }
            }
        }
    }
}

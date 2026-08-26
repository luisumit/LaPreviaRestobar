package com.laprevia.restobar.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.PaymentMethod
import com.laprevia.restobar.domain.Billing
import com.laprevia.restobar.domain.model.Money
import com.laprevia.restobar.domain.repository.FirebaseOrderRepository
import com.laprevia.restobar.domain.repository.FirebaseTableRepository
import com.laprevia.restobar.domain.service.SalesCalculator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CAJA en la PC: cobra un pedido con descuento y vuelto usando la MISMA logica
 * compartida que el celular (aggregate Order + Billing del modulo shared).
 * El flujo es identico al del mozo: applyDiscount -> payWith -> COMPLETED + clearTable.
 */
@Composable
fun CobrarDialog(
    order: Order,
    orderRepo: FirebaseOrderRepository,
    tableRepo: FirebaseTableRepository,
    onClose: (cobrado: Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val total = SalesCalculator.orderTotal(order)

    // Presets de descuento (mismos que el celular)
    val presets = listOf(
        Triple("Sin dto.", 0, null),
        Triple("10%", 10, "Descuento 10%"),
        Triple("15%", 15, "Descuento 15%"),
        Triple("20%", 20, "Descuento 20%"),
        Triple("Happy Hour", 20, "Happy Hour")
    )
    var presetIndex by remember { mutableStateOf(0) }
    var receivedText by remember { mutableStateOf("") }
    var charging by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val percent = presets[presetIndex].second
    val reason = presets[presetIndex].third
    val discount = Billing.discountFromPercent(total, percent)
    val net = Billing.netTotal(total, discount)
    val received = receivedText.replace(",", ".").toDoubleOrNull() ?: 0.0
    val change = Billing.change(received, net)
    val cashReady = received >= net && received > 0.0

    fun charge(method: PaymentMethod, cashReceived: Double?) {
        if (charging) return
        charging = true; error = null
        scope.launch {
            try {
                val paidAt = System.currentTimeMillis()
                val receipt = "LP-" + SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(paidAt))
                val completed = order
                    .applyDiscount(Money(discount), reason ?: "")
                    .payWith(method, cashReceived?.let { Money(it) })
                    .copy(
                        status = OrderStatus.COMPLETED,
                        paidAt = paidAt,
                        receiptNumber = receipt,
                        updatedAt = paidAt
                    )
                orderRepo.updateOrder(completed)
                val tableId = if (order.tableId != 0) order.tableId else order.tableNumber
                runCatching { tableRepo.clearTable(tableId) }
                // Ticket automatico en la termica USB (si esta configurada y activado)
                if (DesktopPrefs.autoPrintTicket && DesktopPrefs.printerName != null) {
                    printOnDesktop(
                        com.laprevia.restobar.data.printer.ReceiptFormatter().customerTicket(completed)
                    )
                }
                onClose(true)
            } catch (e: Exception) {
                error = e.message ?: "Error cobrando"
                charging = false
            }
        }
    }

    Dialog(onDismissRequest = { if (!charging) onClose(false) }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.width(430.dp).padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Cobrar mesa M${order.tableNumber.toString().padStart(2, '0')}",
                    fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFF5F5F5)
                )
                order.items.forEach {
                    Text("  ${it.quantity} x ${it.productName}", color = Color(0xFFF5F5F5).copy(alpha = 0.75f), fontSize = 13.sp)
                }

                Divider(color = Color(0xFF2A2A35))
                Text("Descuento / promocion:", color = Color(0xFFF5F5F5).copy(alpha = 0.8f), fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEachIndexed { i, p ->
                        FilterChip(
                            selected = presetIndex == i,
                            onClick = { presetIndex = i },
                            label = { Text(p.first, fontSize = 11.sp) }
                        )
                    }
                }
                if (discount > 0) {
                    Text("Descuento: -${Money(discount).formatted()}", color = Color(0xFFFF6E40), fontSize = 13.sp)
                }
                Text(
                    "Total a cobrar: ${Money(net).formatted()}",
                    color = Color(0xFFFFB300), fontWeight = FontWeight.Bold, fontSize = 20.sp
                )

                Divider(color = Color(0xFF2A2A35))
                Text("Efectivo:", color = Color(0xFFF5F5F5).copy(alpha = 0.8f), fontSize = 13.sp)
                OutlinedTextField(
                    value = receivedText,
                    onValueChange = { input -> receivedText = input.filter { it.isDigit() || it == '.' || it == ',' } },
                    label = { Text("Con cuanto paga? (S/)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(onClick = { receivedText = String.format(Locale.US, "%.2f", net) }) { Text("Exacto") }
                    listOf(20, 50, 100).forEach { amount ->
                        OutlinedButton(onClick = { receivedText = "$amount.00" }) { Text("$amount") }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Vuelto:", color = Color(0xFFF5F5F5))
                    Text(
                        Money(change).formatted(),
                        fontWeight = FontWeight.Bold, fontSize = 18.sp,
                        color = if (cashReady) Color(0xFF66BB6A) else Color(0xFFFF6E40)
                    )
                }
                Button(
                    onClick = { charge(PaymentMethod.CASH, received) },
                    enabled = cashReady && !charging,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cobrar en EFECTIVO") }

                Divider(color = Color(0xFF2A2A35))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { charge(PaymentMethod.YAPE_PLIN, null) },
                        enabled = !charging, modifier = Modifier.weight(1f)
                    ) { Text("Yape/Plin") }
                    Button(
                        onClick = { charge(PaymentMethod.CARD, null) },
                        enabled = !charging, modifier = Modifier.weight(1f)
                    ) { Text("Tarjeta") }
                }

                error?.let { Text(it, color = Color(0xFFFF5252), fontSize = 13.sp) }
                if (charging) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                TextButton(onClick = { if (!charging) onClose(false) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar", color = Color(0xFFF5F5F5).copy(alpha = 0.7f))
                }
            }
        }
    }
}

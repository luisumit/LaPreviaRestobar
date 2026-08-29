package com.laprevia.restobar.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
        LpDialogCard(width = 440, maxHeight = 680) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "COBRAR ${tableLabel(order.tableNumber)}",
                    fontFamily = BebasFamily, fontSize = 28.sp, letterSpacing = 1.5.sp, color = Lp.Text
                )
                StatusPill("CAJA", Lp.Amber)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                order.items.forEach {
                    Text("${it.quantity} × ${it.productName}", color = Lp.TextSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Divider(color = Lp.Divider)
            Text("DESCUENTO / PROMOCIÓN", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = Lp.TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                presets.forEachIndexed { i, p ->
                    val selected = presetIndex == i
                    Box(
                        Modifier.clip(RoundedCornerShape(999.dp))
                            .background(if (selected) Lp.Amber.copy(alpha = 0.14f) else Color.Transparent)
                            .border(1.dp, if (selected) Lp.Amber.copy(alpha = 0.5f) else Lp.FieldBorder, RoundedCornerShape(999.dp))
                            .lpHover()
                            .clickable { presetIndex = i }
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                    ) {
                        Text(
                            p.first, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (selected) Lp.Amber else Lp.TextDim
                        )
                    }
                }
            }
            if (discount > 0) {
                Text("Descuento: -${Money(discount).formatted()}", color = Lp.Coral, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Total a cobrar:", color = Lp.TextSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(Money(net).formatted(), color = Lp.Amber, fontFamily = BebasFamily, fontSize = 34.sp, letterSpacing = 1.sp)
            }

            Divider(color = Lp.Divider)
            Text("EFECTIVO", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = Lp.TextDim)
            OutlinedTextField(
                value = receivedText,
                onValueChange = { input -> receivedText = input.filter { it.isDigit() || it == '.' || it == ',' } },
                label = { Text("¿Con cuánto paga? (S/)") },
                singleLine = true,
                shape = RoundedCornerShape(13.dp),
                colors = lpFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                QuickAmount("Exacto") { receivedText = String.format(Locale.US, "%.2f", net) }
                listOf(20, 50, 100).forEach { amount ->
                    QuickAmount("S/ $amount") { receivedText = "$amount.00" }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Vuelto:", color = Lp.Text, fontWeight = FontWeight.SemiBold)
                Text(
                    Money(change).formatted(),
                    fontWeight = FontWeight.ExtraBold, fontSize = 19.sp,
                    color = if (cashReady) Lp.Green else Lp.Coral
                )
            }
            Box(
                Modifier.fillMaxWidth().height(50.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (cashReady && !charging) Brush.linearGradient(listOf(Lp.Amber, Lp.AmberDeep))
                        else Brush.linearGradient(listOf(Lp.Amber.copy(alpha = 0.3f), Lp.AmberDeep.copy(alpha = 0.3f)))
                    )
                    .lpHover(0.10f, enabled = cashReady && !charging)
                    .clickable(enabled = cashReady && !charging) { charge(PaymentMethod.CASH, received) },
                contentAlignment = Alignment.Center
            ) {
                Text("COBRAR EN EFECTIVO", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 1.5.sp, color = Lp.OnAccent)
            }

            Divider(color = Lp.Divider)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PayMethodButton("YAPE / PLIN", Lp.Green, enabled = !charging, modifier = Modifier.weight(1f)) {
                    charge(PaymentMethod.YAPE_PLIN, null)
                }
                PayMethodButton("TARJETA", Lp.TextSoft, enabled = !charging, modifier = Modifier.weight(1f)) {
                    charge(PaymentMethod.CARD, null)
                }
            }

            error?.let { Text(it, color = Lp.Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            if (charging) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Lp.Amber, trackColor = Lp.Divider)
            }
            TextButton(onClick = { if (!charging) onClose(false) }, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar", color = Lp.TextDim, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun QuickAmount(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Lp.FieldBorder),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
    ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Lp.TextSoft) }
}

@Composable
private fun PayMethodButton(label: String, accent: Color, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(46.dp).clip(RoundedCornerShape(13.dp))
            .background(accent.copy(alpha = if (enabled) 0.14f else 0.06f))
            .border(1.dp, accent.copy(alpha = if (enabled) 0.4f else 0.15f), RoundedCornerShape(13.dp))
            .lpHover(0.06f, enabled = enabled)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.sp, color = accent.copy(alpha = if (enabled) 1f else 0.5f))
    }
}

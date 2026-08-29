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
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.PaymentMethod
import com.laprevia.restobar.data.printer.EscPosEncoder
import com.laprevia.restobar.data.printer.PaperWidth
import com.laprevia.restobar.data.printer.ReceiptDocument
import com.laprevia.restobar.data.printer.ReceiptFormatter

/** Imprime un documento con la impresora configurada del panel. */
fun printOnDesktop(document: ReceiptDocument): Result<Unit> {
    val printer = DesktopPrefs.printerName
        ?: return Result.failure(IllegalStateException("No hay impresora configurada"))
    val bytes = EscPosEncoder.encode(document, DesktopPrefs.paperWidth)
    return DesktopPrinter.printRaw(bytes, printer)
}

private fun sampleOrder(): Order = Order(
    id = "demo",
    tableId = 5,
    tableNumber = 5,
    items = listOf(
        OrderItem(productName = "Chilcano de pisco", quantity = 2, unitPrice = 15.0, subtotal = 30.0),
        OrderItem(productName = "Salchipapa personal", quantity = 1, unitPrice = 12.5, subtotal = 12.5)
    ),
    status = OrderStatus.COMPLETED,
    total = 42.5,
    waiterName = "Caja PC",
    paymentMethod = PaymentMethod.CASH,
    paidAt = System.currentTimeMillis(),
    receiptNumber = "LP-PRUEBA-PC",
    amountReceived = 50.0,
    changeGiven = 7.5
)

@Composable
fun PrinterSettingsDialog(onClose: () -> Unit) {
    val printers = remember { DesktopPrinter.listPrinters() }
    var selected by remember { mutableStateOf(DesktopPrefs.printerName) }
    var paper by remember { mutableStateOf(DesktopPrefs.paperWidth) }
    var autoTicket by remember { mutableStateOf(DesktopPrefs.autoPrintTicket) }
    var status by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onClose) {
        LpDialogCard(width = 460, maxHeight = 620) {
            Text("IMPRESORA TÉRMICA (USB)", fontFamily = BebasFamily, fontSize = 26.sp, letterSpacing = 1.5.sp, color = Lp.Text)
            Text(
                "Elige la impresora instalada en Windows. Para una térmica USB usa su driver o \"Generic / Text Only\".",
                color = Lp.TextDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
            )

            if (printers.isEmpty()) {
                Text("No se encontraron impresoras instaladas.", color = Lp.Coral, fontWeight = FontWeight.SemiBold)
            } else {
                printers.forEach { name ->
                    val isSel = name == selected
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) Lp.Amber.copy(alpha = 0.12f) else Lp.Field)
                            .border(1.dp, if (isSel) Lp.Amber.copy(alpha = 0.5f) else Lp.FieldBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                selected = name
                                DesktopPrefs.printerName = name
                                status = "Impresora guardada: $name"
                            }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isSel) "✓  $name" else name, fontSize = 13.sp,
                            fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSel) Lp.Amber else Lp.TextSoft
                        )
                    }
                }
            }

            Divider(color = Lp.Divider)
            Text("ANCHO DE PAPEL", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, color = Lp.TextDim)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PaperWidth.entries.forEach { width ->
                    val sel = paper == width
                    Box(
                        Modifier.clip(RoundedCornerShape(999.dp))
                            .background(if (sel) Lp.Amber.copy(alpha = 0.14f) else Color.Transparent)
                            .border(1.dp, if (sel) Lp.Amber.copy(alpha = 0.5f) else Lp.FieldBorder, RoundedCornerShape(999.dp))
                            .clickable { paper = width; DesktopPrefs.paperWidth = width }
                            .padding(horizontal = 15.dp, vertical = 8.dp)
                    ) {
                        Text(width.label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (sel) Lp.Amber else Lp.TextDim)
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Imprimir ticket al cobrar", color = Lp.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Automático tras cada cobro desde la PC", color = Lp.TextDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
                Switch(checked = autoTicket, onCheckedChange = { autoTicket = it; DesktopPrefs.autoPrintTicket = it })
            }

            Divider(color = Lp.Divider)
            Box(
                Modifier.fillMaxWidth().height(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (selected != null) Brush.linearGradient(listOf(Lp.Amber, Lp.AmberDeep))
                        else Brush.linearGradient(listOf(Lp.Amber.copy(alpha = 0.3f), Lp.AmberDeep.copy(alpha = 0.3f)))
                    )
                    .clickable(enabled = selected != null) {
                        val result = printOnDesktop(ReceiptFormatter().customerTicket(sampleOrder()))
                        status = result.fold(
                            onSuccess = { "✅ Ticket de prueba enviado a la impresora" },
                            onFailure = { "❌ ${it.message}" }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("IMPRIMIR TICKET DE PRUEBA", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.5.sp, color = Lp.OnAccent)
            }

            status?.let { Text(it, color = Lp.Amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }

            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar", color = Lp.TextDim, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

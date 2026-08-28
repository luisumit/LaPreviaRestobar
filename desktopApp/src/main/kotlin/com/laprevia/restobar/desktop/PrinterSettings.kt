package com.laprevia.restobar.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E28)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.width(460.dp).heightIn(max = 620.dp)
                    .padding(20.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Impresora termica (USB)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFF5F5F5))
                Text(
                    "Elige la impresora instalada en Windows. Para una termica USB usa su driver o \"Generic / Text Only\".",
                    color = Color(0xFFF5F5F5).copy(alpha = 0.6f), fontSize = 12.sp
                )

                if (printers.isEmpty()) {
                    Text("No se encontraron impresoras instaladas.", color = Color(0xFFFF6E40))
                } else {
                    printers.forEach { name ->
                        val isSel = name == selected
                        OutlinedButton(
                            onClick = {
                                selected = name
                                DesktopPrefs.printerName = name
                                status = "Impresora guardada: $name"
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (isSel) ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFB300).copy(alpha = 0.18f))
                            else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(if (isSel) "✓ $name" else name, fontSize = 13.sp)
                        }
                    }
                }

                Divider(color = Color(0xFF2A2A35))
                Text("Ancho de papel:", color = Color(0xFFF5F5F5).copy(alpha = 0.8f), fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaperWidth.entries.forEach { width ->
                        FilterChip(
                            selected = paper == width,
                            onClick = { paper = width; DesktopPrefs.paperWidth = width },
                            label = { Text(width.label) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Imprimir ticket al cobrar", color = Color(0xFFF5F5F5), fontSize = 14.sp)
                        Text("Automatico tras cada cobro desde la PC", color = Color(0xFFF5F5F5).copy(alpha = 0.55f), fontSize = 11.sp)
                    }
                    Switch(checked = autoTicket, onCheckedChange = { autoTicket = it; DesktopPrefs.autoPrintTicket = it })
                }

                Divider(color = Color(0xFF2A2A35))
                Button(
                    onClick = {
                        val result = printOnDesktop(ReceiptFormatter().customerTicket(sampleOrder()))
                        status = result.fold(
                            onSuccess = { "✅ Ticket de prueba enviado a la impresora" },
                            onFailure = { "❌ ${it.message}" }
                        )
                    },
                    enabled = selected != null,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Imprimir ticket de PRUEBA") }

                status?.let { Text(it, color = Color(0xFFFFB300), fontSize = 12.sp) }

                TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text("Cerrar", color = Color(0xFFF5F5F5).copy(alpha = 0.7f))
                }
            }
        }
    }
}

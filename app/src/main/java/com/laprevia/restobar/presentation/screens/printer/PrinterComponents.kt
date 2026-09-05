package com.laprevia.restobar.presentation.screens.printer

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.laprevia.restobar.data.printer.LineAlign
import com.laprevia.restobar.data.printer.PaperWidth
import com.laprevia.restobar.data.printer.PrinterConfig
import com.laprevia.restobar.data.printer.PrinterDevice
import com.laprevia.restobar.data.printer.ReceiptDocument
import com.laprevia.restobar.data.printer.ReceiptLine

/** Fila con etiqueta, descripcion breve y un Switch. */
@Composable
private fun ToggleRow(
    label: String,
    hint: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            Text(hint, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 11.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Vista previa del recibo tal como saldra en papel, en fuente monoespaciada. */
@Composable
fun ReceiptPaper(document: ReceiptDocument, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xFFF7F7F2))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            document.lines.forEach { line ->
                when (line) {
                    is ReceiptLine.Text -> Text(
                        text = line.text,
                        color = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (line.bold) FontWeight.Bold else FontWeight.Normal,
                        fontSize = if (line.big) 17.sp else 12.sp,
                        textAlign = when (line.align) {
                            LineAlign.LEFT -> TextAlign.Start
                            LineAlign.CENTER -> TextAlign.Center
                            LineAlign.RIGHT -> TextAlign.End
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    is ReceiptLine.TwoCols -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            line.left,
                            color = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (line.bold) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                        Text(
                            line.right,
                            color = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (line.bold) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }

                    is ReceiptLine.Divider -> Text(
                        text = "-".repeat(34),
                        color = androidx.compose.ui.graphics.Color(0xFF888888),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )

                    is ReceiptLine.Feed -> Spacer(modifier = Modifier.height((line.lines * 6).dp))
                }
            }
        }
    }
}

/** Dialogo que muestra la vista previa de un documento y permite imprimirlo. */
@Composable
fun ReceiptPreviewDialog(
    title: String,
    document: ReceiptDocument,
    isPrinting: Boolean,
    statusMessage: String?,
    onPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                ReceiptPaper(
                    document = document,
                    modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())
                )
                statusMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cerrar")
                    }
                    Button(onClick = onPrint, modifier = Modifier.weight(1f), enabled = !isPrinting) {
                        if (isPrinting) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Imprimir")
                        }
                    }
                }
            }
        }
    }
}

/** Dialogo de ajustes de impresora: elegir dispositivo emparejado, ancho de papel y prueba. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterSettingsDialog(
    config: PrinterConfig,
    hasPermission: Boolean,
    isPrinting: Boolean,
    statusMessage: String?,
    onLoadPrinters: () -> List<PrinterDevice>,
    onSelectPrinter: (PrinterDevice) -> Unit,
    onSelectPaper: (PaperWidth) -> Unit,
    onToggleAutoComanda: (Boolean) -> Unit,
    onToggleAutoTicket: (Boolean) -> Unit,
    onSampleComanda: () -> ReceiptDocument,
    onSampleTicket: () -> ReceiptDocument,
    onPrintDocument: (ReceiptDocument) -> Unit,
    onTestPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    var permissionGranted by remember { mutableStateOf(hasPermission) }
    var printers by remember { mutableStateOf(if (hasPermission) onLoadPrinters() else emptyList()) }
    var samplePreview by remember { mutableStateOf<Pair<String, ReceiptDocument>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        if (granted) printers = onLoadPrinters()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Impresora termica", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                Text(
                    if (config.isConfigured) "Actual: ${config.name}" else "Ninguna impresora seleccionada",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )

                Text("Ancho de papel", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PaperWidth.entries.forEachIndexed { index, width ->
                        SegmentedButton(
                            selected = config.paperWidth == width,
                            onClick = { onSelectPaper(width) },
                            shape = SegmentedButtonDefaults.itemShape(index, PaperWidth.entries.size)
                        ) { Text(width.label) }
                    }
                }

                HorizontalDivider()

                ToggleRow(
                    label = "Auto-imprimir comanda",
                    hint = "Al llegar un pedido nuevo (dispositivo de cocina)",
                    checked = config.autoPrintComanda,
                    onCheckedChange = onToggleAutoComanda
                )
                ToggleRow(
                    label = "Auto-imprimir ticket",
                    hint = "Al cobrar y liberar la mesa (dispositivo del mozo)",
                    checked = config.autoPrintTicket,
                    onCheckedChange = onToggleAutoTicket
                )

                HorizontalDivider()

                Text("Ver ejemplo (datos de muestra)", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { samplePreview = "Ejemplo - Comanda" to onSampleComanda() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Comanda") }
                    OutlinedButton(
                        onClick = { samplePreview = "Ejemplo - Ticket" to onSampleTicket() },
                        modifier = Modifier.weight(1f)
                    ) { Text("Ticket") }
                }

                HorizontalDivider()

                if (!permissionGranted) {
                    Text(
                        "Se necesita permiso de Bluetooth para ver las impresoras emparejadas.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                            } else {
                                permissionGranted = true
                                printers = onLoadPrinters()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Dar permiso de Bluetooth") }
                } else {
                    Text("Impresoras emparejadas", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    if (printers.isEmpty()) {
                        Text(
                            "No hay impresoras emparejadas. Emparejala primero en los Ajustes de Bluetooth de Android.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontSize = 13.sp
                        )
                        OutlinedButton(onClick = { printers = onLoadPrinters() }, modifier = Modifier.fillMaxWidth()) {
                            Text("Actualizar lista")
                        }
                    } else {
                        printers.forEach { device ->
                            val selected = device.mac == config.mac
                            OutlinedButton(
                                onClick = { onSelectPrinter(device) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = if (selected) {
                                    ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                }
                            ) {
                                Text(if (selected) "✓ ${device.name}" else device.name)
                            }
                        }
                    }
                }

                statusMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cerrar") }
                    Button(
                        onClick = onTestPrint,
                        modifier = Modifier.weight(1f),
                        enabled = config.isConfigured && !isPrinting
                    ) {
                        if (isPrinting) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Prueba")
                        }
                    }
                }
            }
        }
    }

    samplePreview?.let { (title, document) ->
        ReceiptPreviewDialog(
            title = title,
            document = document,
            isPrinting = isPrinting,
            statusMessage = statusMessage,
            onPrint = { onPrintDocument(document) },
            onDismiss = { samplePreview = null }
        )
    }
}

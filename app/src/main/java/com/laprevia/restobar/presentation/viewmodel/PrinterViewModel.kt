package com.laprevia.restobar.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laprevia.restobar.data.local.datastore.PreferencesManager
import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.OrderItem
import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.data.model.PaymentMethod
import com.laprevia.restobar.data.printer.BluetoothPrinterManager
import com.laprevia.restobar.data.printer.EscPosEncoder
import com.laprevia.restobar.data.printer.LineAlign
import com.laprevia.restobar.data.printer.PaperWidth
import com.laprevia.restobar.data.printer.PrinterConfig
import com.laprevia.restobar.data.printer.PrinterDevice
import com.laprevia.restobar.data.printer.ReceiptDocument
import com.laprevia.restobar.data.printer.ReceiptFormatter
import com.laprevia.restobar.data.printer.ReceiptLine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrinterViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val formatter: ReceiptFormatter,
    private val printerManager: BluetoothPrinterManager
) : ViewModel() {

    val config: StateFlow<PrinterConfig> = preferencesManager.printerConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PrinterConfig())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isPrinting = MutableStateFlow(false)
    val isPrinting: StateFlow<Boolean> = _isPrinting.asStateFlow()

    fun hasBluetoothPermission(): Boolean = printerManager.hasConnectPermission()

    fun pairedPrinters(): List<PrinterDevice> = printerManager.pairedPrinters()

    fun selectPrinter(device: PrinterDevice) {
        viewModelScope.launch {
            preferencesManager.savePrinter(device.mac, device.name)
            _statusMessage.value = "Impresora '${device.name}' seleccionada"
        }
    }

    fun setPaperWidth(width: PaperWidth) {
        viewModelScope.launch { preferencesManager.savePaperWidth(width) }
    }

    fun setAutoPrintComanda(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setAutoPrintComanda(enabled) }
    }

    fun setAutoPrintTicket(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setAutoPrintTicket(enabled) }
    }

    fun buildComanda(order: Order): ReceiptDocument = formatter.kitchenComanda(order)

    fun buildTicket(order: Order): ReceiptDocument = formatter.customerTicket(order)

    // Pedido ficticio para ver el formato sin necesidad de datos reales.
    private fun sampleOrder(): Order = Order(
        id = "demo",
        tableId = 5,
        tableNumber = 5,
        items = listOf(
            OrderItem(productName = "Chilcano de pisco", quantity = 2, unitPrice = 15.0, subtotal = 30.0),
            OrderItem(productName = "Salchipapa personal", quantity = 1, unitPrice = 12.5, subtotal = 12.5),
            OrderItem(productName = "Alitas BBQ (8u)", quantity = 1, unitPrice = 24.0, subtotal = 24.0)
        ),
        status = OrderStatus.COMPLETED,
        total = 56.5, // neto tras descuento (subtotal 66.5 - 10.0)
        waiterName = "Mozo demo",
        notes = "Sin aji en las alitas",
        paymentMethod = PaymentMethod.CASH,
        paidAt = System.currentTimeMillis(),
        receiptNumber = "LP-DEMO-0001",
        amountReceived = 100.0,
        changeGiven = 43.5,
        discountAmount = 10.0,
        discountReason = "Happy Hour"
    )

    fun sampleComanda(): ReceiptDocument = formatter.kitchenComanda(sampleOrder())

    fun sampleTicket(): ReceiptDocument = formatter.customerTicket(sampleOrder())

    /** Envia un documento a la impresora configurada. */
    fun print(document: ReceiptDocument) {
        val current = config.value
        if (!current.isConfigured) {
            _statusMessage.value = "Primero selecciona una impresora en Ajustes"
            return
        }
        if (!printerManager.hasConnectPermission()) {
            _statusMessage.value = "Falta permiso de Bluetooth"
            return
        }
        viewModelScope.launch {
            _isPrinting.value = true
            val bytes = EscPosEncoder.encode(document, current.paperWidth)
            val result = printerManager.printBytes(bytes, current.mac!!)
            _statusMessage.value = result.fold(
                onSuccess = { "Enviado a la impresora" },
                onFailure = { it.message ?: "Error al imprimir" }
            )
            _isPrinting.value = false
        }
    }

    fun testPrint() {
        val demo = ReceiptDocument(
            listOf(
                ReceiptLine.Text("LA PREVIA RESTOBAR", bold = true, big = true, align = LineAlign.CENTER),
                ReceiptLine.Text("Prueba de impresion", align = LineAlign.CENTER),
                ReceiptLine.Divider,
                ReceiptLine.TwoCols("Producto demo", "S/ 10.00"),
                ReceiptLine.Divider,
                ReceiptLine.Text("Si lees esto, funciona!", align = LineAlign.CENTER),
                ReceiptLine.Feed(3)
            )
        )
        print(demo)
    }

    fun clearStatus() {
        _statusMessage.value = null
    }
}

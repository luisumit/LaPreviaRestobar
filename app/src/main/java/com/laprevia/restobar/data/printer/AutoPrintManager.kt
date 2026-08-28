package com.laprevia.restobar.data.printer

import com.laprevia.restobar.data.local.datastore.PreferencesManager
import com.laprevia.restobar.data.model.Order
import kotlinx.coroutines.flow.first
import java.util.Collections

/**
 * Auto-impresion de comandas y tickets segun la configuracion del dispositivo.
 * Es "dispara y olvida": si no hay impresora, el toggle esta apagado o ya se
 * imprimio ese documento, no hace nada y nunca lanza excepciones al llamador.
 */
class AutoPrintManager constructor(
    private val preferencesManager: PreferencesManager,
    private val formatter: ReceiptFormatter,
    private val printerManager: BluetoothPrinterManager
) {
    // Momento en que arranco la app: evita reimprimir el backlog de pedidos
    // ya existentes cuando el listener en tiempo real re-emite lo viejo.
    private val sessionStart = System.currentTimeMillis()

    // Guarda que cada documento se imprima una sola vez por sesion.
    private val printedKeys = Collections.synchronizedSet(mutableSetOf<String>())

    /** Imprime la comanda si esta habilitado y el pedido es realmente nuevo. */
    suspend fun autoPrintComanda(order: Order) {
        val config = preferencesManager.printerConfig.first()
        if (!config.autoPrintComanda || !config.isConfigured) return
        if (order.createdAt < sessionStart) return // pedido previo al arranque
        if (order.tableNumber <= 0) return
        if (!printedKeys.add("comanda:${order.id}")) return
        runCatching {
            val bytes = EscPosEncoder.encode(formatter.kitchenComanda(order), config.paperWidth)
            printerManager.printBytes(bytes, config.mac!!)
        }
    }

    /** Imprime el ticket del cliente si esta habilitado (disparado al cobrar). */
    suspend fun autoPrintTicket(order: Order) {
        val config = preferencesManager.printerConfig.first()
        if (!config.autoPrintTicket || !config.isConfigured) return
        if (!printedKeys.add("ticket:${order.id}")) return
        runCatching {
            val bytes = EscPosEncoder.encode(formatter.customerTicket(order), config.paperWidth)
            printerManager.printBytes(bytes, config.mac!!)
        }
    }
}

package com.laprevia.restobar.data.printer

import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.data.model.PaymentMethod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Construye los documentos imprimibles a partir de una orden.
 * No depende de Android ni de Bluetooth: solo transforma datos en lineas.
 */
class ReceiptFormatter @Inject constructor() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)

    // Estandariza el nombre de la mesa: 1 -> M01, 12 -> M12
    private fun tableLabel(number: Int): String = "M%02d".format(number)

    private fun orderTotal(order: Order): Double =
        order.total.takeIf { it > 0.0 } ?: order.items.sumOf { item ->
            item.subtotal.takeIf { it > 0.0 } ?: (item.unitPrice * item.quantity)
        }

    /** Comanda para la cocina: sin precios, resalta cantidades y notas. */
    fun kitchenComanda(order: Order, businessName: String = "LA PREVIA RESTOBAR"): ReceiptDocument {
        val lines = buildList {
            add(ReceiptLine.Text(businessName, bold = true, align = LineAlign.CENTER))
            add(ReceiptLine.Text("*** COMANDA COCINA ***", bold = true, align = LineAlign.CENTER))
            add(ReceiptLine.Divider)
            add(ReceiptLine.Text("Mesa: ${tableLabel(order.tableNumber)}", bold = true, big = true))
            order.waiterName?.takeIf { it.isNotBlank() }?.let { add(ReceiptLine.Text("Mozo: $it")) }
            add(ReceiptLine.Text("Hora: ${dateFormat.format(Date(order.createdAt))}"))
            add(ReceiptLine.Divider)
            order.items.forEach { item ->
                add(ReceiptLine.Text("${item.quantity} x ${item.productName}", bold = true))
            }
            if (!order.notes.isNullOrBlank()) {
                add(ReceiptLine.Divider)
                add(ReceiptLine.Text("Nota: ${order.notes}"))
            }
            add(ReceiptLine.Divider)
            add(ReceiptLine.Text("Items: ${order.items.sumOf { it.quantity }}", align = LineAlign.CENTER))
            add(ReceiptLine.Feed(3))
        }
        return ReceiptDocument(lines)
    }

    /** Ticket para el cliente: con precios, total, metodo de pago y numero de ticket. */
    fun customerTicket(order: Order, businessName: String = "LA PREVIA RESTOBAR"): ReceiptDocument {
        val total = orderTotal(order)
        val lines = buildList {
            add(ReceiptLine.Text(businessName, bold = true, big = true, align = LineAlign.CENTER))
            add(ReceiptLine.Text("Ticket de venta", align = LineAlign.CENTER))
            add(ReceiptLine.Divider)
            order.receiptNumber?.takeIf { it.isNotBlank() }?.let { add(ReceiptLine.Text("Ticket: $it")) }
            add(ReceiptLine.Text("Mesa: ${tableLabel(order.tableNumber)}"))
            order.waiterName?.takeIf { it.isNotBlank() }?.let { add(ReceiptLine.Text("Mozo: $it")) }
            add(ReceiptLine.Text("Fecha: ${dateFormat.format(Date(order.paidAt ?: System.currentTimeMillis()))}"))
            add(ReceiptLine.Divider)
            order.items.forEach { item ->
                val unit = item.unitPrice.takeIf { it > 0.0 } ?: item.productPrice
                val sub = item.subtotal.takeIf { it > 0.0 } ?: (unit * item.quantity)
                add(ReceiptLine.Text("${item.productName}"))
                add(ReceiptLine.TwoCols("  ${item.quantity} x ${money(unit)}", "S/ ${money(sub)}"))
            }
            add(ReceiptLine.Divider)
            val discount = order.discountAmount ?: 0.0
            if (discount > 0.0) {
                val subtotal = order.items.sumOf { item ->
                    item.subtotal.takeIf { it > 0.0 } ?: (item.unitPrice * item.quantity)
                }
                add(ReceiptLine.TwoCols("Subtotal", "S/ ${money(subtotal)}"))
                val label = order.discountReason?.takeIf { it.isNotBlank() } ?: "Descuento"
                add(ReceiptLine.TwoCols(label, "-S/ ${money(discount)}"))
            }
            add(ReceiptLine.TwoCols("TOTAL", "S/ ${money(total)}", bold = true))
            add(ReceiptLine.Text("Pago: ${order.paymentMethod.label}"))
            if (order.paymentMethod == PaymentMethod.CASH && (order.amountReceived ?: 0.0) > 0.0) {
                add(ReceiptLine.TwoCols("Recibido", "S/ ${money(order.amountReceived ?: 0.0)}"))
                add(ReceiptLine.TwoCols("Vuelto", "S/ ${money(order.changeGiven ?: 0.0)}"))
            }
            add(ReceiptLine.Divider)
            add(ReceiptLine.Text("Gracias por su visita!", align = LineAlign.CENTER))
            add(ReceiptLine.Feed(3))
        }
        return ReceiptDocument(lines)
    }
}

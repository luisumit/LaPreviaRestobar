package com.laprevia.restobar.data.printer

/**
 * Ancho del papel de la impresora termica. Los caracteres por linea dependen del
 * ancho: 58 mm ~ 32 caracteres, 80 mm ~ 48 caracteres (fuente estandar Font A).
 */
enum class PaperWidth(val label: String, val charsPerLine: Int) {
    MM_58("58 mm", 32),
    MM_80("80 mm", 48);

    companion object {
        fun fromString(value: String?): PaperWidth = entries.find { it.name == value } ?: MM_58
    }
}

enum class LineAlign { LEFT, CENTER, RIGHT }

/** Una linea del documento a imprimir. El encoder y la vista previa la interpretan igual. */
sealed interface ReceiptLine {
    data class Text(
        val text: String,
        val bold: Boolean = false,
        val big: Boolean = false,
        val align: LineAlign = LineAlign.LEFT
    ) : ReceiptLine

    /** Dos columnas: etiqueta a la izquierda, valor a la derecha (para items y totales). */
    data class TwoCols(
        val left: String,
        val right: String,
        val bold: Boolean = false
    ) : ReceiptLine

    /** Linea de guiones que ocupa todo el ancho. */
    data object Divider : ReceiptLine

    /** Avance de papel (lineas en blanco). */
    data class Feed(val lines: Int = 1) : ReceiptLine
}

data class ReceiptDocument(val lines: List<ReceiptLine>)

/** Impresora emparejada por Bluetooth. */
data class PrinterDevice(val name: String, val mac: String)

/** Configuracion guardada de la impresora. */
data class PrinterConfig(
    val mac: String? = null,
    val name: String? = null,
    val paperWidth: PaperWidth = PaperWidth.MM_58,
    val autoPrintComanda: Boolean = false,
    val autoPrintTicket: Boolean = false
) {
    val isConfigured: Boolean get() = !mac.isNullOrBlank()
}

package com.laprevia.restobar.desktop

import com.laprevia.restobar.data.printer.PaperWidth
import java.io.File
import java.util.Properties
import javax.print.DocFlavor
import javax.print.PrintServiceLookup
import javax.print.SimpleDoc

/**
 * Impresion RAW (ESC/POS) hacia una impresora instalada en Windows — tipicamente
 * una termica USB (con su driver o el driver "Generic / Text Only").
 * Los bytes van directo al spooler, sin procesar: la termica los interpreta.
 */
object DesktopPrinter {

    /** Impresoras instaladas en el sistema. */
    fun listPrinters(): List<String> =
        PrintServiceLookup.lookupPrintServices(null, null).map { it.name }

    /** Envia bytes ESC/POS crudos a la impresora indicada. */
    fun printRaw(bytes: ByteArray, printerName: String): Result<Unit> = runCatching {
        val service = PrintServiceLookup.lookupPrintServices(null, null)
            .firstOrNull { it.name.equals(printerName, ignoreCase = true) }
            ?: error("Impresora '$printerName' no encontrada")
        val doc = SimpleDoc(bytes, DocFlavor.BYTE_ARRAY.AUTOSENSE, null)
        service.createPrintJob().print(doc, null)
    }
}

/**
 * Configuracion de impresora del panel de escritorio, persistida en
 * ~/.laprevia-desktop.properties (sobrevive reinicios de la app).
 */
object DesktopPrefs {

    private val file = File(System.getProperty("user.home"), ".laprevia-desktop.properties")

    var printerName: String?
        get() = load().getProperty("printer.name")?.takeIf { it.isNotBlank() }
        set(value) = save("printer.name", value ?: "")

    var paperWidth: PaperWidth
        get() = PaperWidth.fromString(load().getProperty("printer.paper"))
        set(value) = save("printer.paper", value.name)

    var autoPrintTicket: Boolean
        get() = load().getProperty("printer.autoTicket", "true") == "true"
        set(value) = save("printer.autoTicket", value.toString())

    private fun load(): Properties = Properties().apply {
        runCatching { if (file.exists()) file.inputStream().use { load(it) } }
    }

    private fun save(key: String, value: String) {
        runCatching {
            val props = load()
            props.setProperty(key, value)
            file.outputStream().use { props.store(it, "La Previa Restobar - Panel de Escritorio") }
        }
    }
}

package com.laprevia.restobar

import com.laprevia.restobar.data.printer.EscPosEncoder
import com.laprevia.restobar.data.printer.LineAlign
import com.laprevia.restobar.data.printer.PaperWidth
import com.laprevia.restobar.data.printer.ReceiptDocument
import com.laprevia.restobar.data.printer.ReceiptLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas del encoder ESC/POS: normalizacion de acentos, ancho de linea,
 * columnas y comando de corte. Se decodifica en ISO-8859-1 para inspeccionar texto.
 */
class EscPosEncoderTest {

    private fun encodeText(doc: ReceiptDocument, width: PaperWidth = PaperWidth.MM_58): String {
        return String(EscPosEncoder.encode(doc, width), Charsets.ISO_8859_1)
    }

    @Test
    fun `quita acentos y enie`() {
        val doc = ReceiptDocument(listOf(ReceiptLine.Text("Café ñoño áéíóú")))
        val text = encodeText(doc)
        assertTrue("Debe contener el texto sin acentos", text.contains("Cafe nono aeiou"))
        assertFalse("No debe quedar la enie", text.contains("ñ"))
        assertFalse("No debe quedar acento", text.contains("é"))
    }

    @Test
    fun `divider ocupa el ancho de 58mm (32 guiones)`() {
        val text = encodeText(ReceiptDocument(listOf(ReceiptLine.Divider)))
        assertTrue("Debe tener 32 guiones seguidos", text.contains("-".repeat(32)))
    }

    @Test
    fun `divider de 80mm usa 48 guiones`() {
        val text = encodeText(ReceiptDocument(listOf(ReceiptLine.Divider)), PaperWidth.MM_80)
        assertTrue(text.contains("-".repeat(48)))
    }

    @Test
    fun `dos columnas muestra etiqueta y valor`() {
        val doc = ReceiptDocument(listOf(ReceiptLine.TwoCols("TOTAL", "S/ 50.00", bold = true)))
        val text = encodeText(doc)
        assertTrue(text.contains("TOTAL"))
        assertTrue(text.contains("S/ 50.00"))
    }

    @Test
    fun `texto largo se conserva completo aunque se parta en lineas`() {
        val largo = "Salchipapa familiar con doble salsa y extra queso derretido"
        val text = encodeText(ReceiptDocument(listOf(ReceiptLine.Text(largo))))
        // El contenido no se pierde al hacer wrap
        assertTrue(text.contains("Salchipapa"))
        assertTrue(text.contains("queso"))
    }

    @Test
    fun `documento inicia con ESC arroba y termina con corte de papel`() {
        val bytes = EscPosEncoder.encode(ReceiptDocument(listOf(ReceiptLine.Text("Hola"))), PaperWidth.MM_58)
        // Init: ESC @ = 0x1B 0x40
        assertEquals(0x1B.toByte(), bytes[0])
        assertEquals(0x40.toByte(), bytes[1])
        // Corte al final: GS V 0 = 0x1D 0x56 0x00
        assertEquals(0x1D.toByte(), bytes[bytes.size - 3])
        assertEquals(0x56.toByte(), bytes[bytes.size - 2])
        assertEquals(0x00.toByte(), bytes[bytes.size - 1])
    }

    @Test
    fun `texto centrado sigue conteniendo el contenido`() {
        val doc = ReceiptDocument(listOf(ReceiptLine.Text("LA PREVIA", align = LineAlign.CENTER, bold = true)))
        assertTrue(encodeText(doc).contains("LA PREVIA"))
    }
}

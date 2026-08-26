package com.laprevia.restobar.data.printer

import java.io.ByteArrayOutputStream

/**
 * Traduce un [ReceiptDocument] a bytes ESC/POS que entiende una impresora termica.
 * El texto se normaliza a ASCII (sin acentos) porque muchas impresoras baratas no
 * soportan UTF-8 en su fuente por defecto.
 */
object EscPosEncoder {

    private val ESC = 0x1B.toByte()
    private val GS = 0x1D.toByte()
    private const val LF = 0x0A

    fun encode(doc: ReceiptDocument, paperWidth: PaperWidth): ByteArray {
        val width = paperWidth.charsPerLine
        val out = ByteArrayOutputStream()

        // Inicializar impresora
        out.write(byteArrayOf(ESC, '@'.code.toByte()))

        doc.lines.forEach { line ->
            when (line) {
                is ReceiptLine.Text -> writeText(out, line, width)
                is ReceiptLine.TwoCols -> writeTwoCols(out, line, width)
                is ReceiptLine.Divider -> {
                    resetStyle(out)
                    out.write(sanitize("-".repeat(width)).toByteArray(Charsets.ISO_8859_1))
                    out.write(LF)
                }
                is ReceiptLine.Feed -> repeat(line.lines.coerceAtLeast(1)) { out.write(LF) }
            }
        }

        // Avance final y corte de papel
        out.write(byteArrayOf(ESC, 'd'.code.toByte(), 3))
        out.write(byteArrayOf(GS, 'V'.code.toByte(), 0x00))
        return out.toByteArray()
    }

    private fun writeText(out: ByteArrayOutputStream, line: ReceiptLine.Text, width: Int) {
        setAlign(out, line.align)
        setBold(out, line.bold)
        setBig(out, line.big)
        val effectiveWidth = if (line.big) width / 2 else width
        wrap(sanitize(line.text), effectiveWidth).forEach { chunk ->
            out.write(chunk.toByteArray(Charsets.ISO_8859_1))
            out.write(LF)
        }
        resetStyle(out)
    }

    private fun writeTwoCols(out: ByteArrayOutputStream, line: ReceiptLine.TwoCols, width: Int) {
        setAlign(out, LineAlign.LEFT)
        setBold(out, line.bold)
        val left = sanitize(line.left)
        val right = sanitize(line.right)
        val space = (width - left.length - right.length).coerceAtLeast(1)
        val row = if (left.length + right.length + 1 > width) {
            // No caben en una linea: derecha debajo, alineada al borde
            left + "\n" + " ".repeat((width - right.length).coerceAtLeast(0)) + right
        } else {
            left + " ".repeat(space) + right
        }
        row.split("\n").forEach {
            out.write(it.toByteArray(Charsets.ISO_8859_1))
            out.write(LF)
        }
        resetStyle(out)
    }

    private fun setAlign(out: ByteArrayOutputStream, align: LineAlign) {
        val n = when (align) {
            LineAlign.LEFT -> 0
            LineAlign.CENTER -> 1
            LineAlign.RIGHT -> 2
        }
        out.write(byteArrayOf(ESC, 'a'.code.toByte(), n.toByte()))
    }

    private fun setBold(out: ByteArrayOutputStream, bold: Boolean) {
        out.write(byteArrayOf(ESC, 'E'.code.toByte(), if (bold) 1 else 0))
    }

    private fun setBig(out: ByteArrayOutputStream, big: Boolean) {
        // GS ! n : nibble alto = ancho, nibble bajo = alto. 0x11 = doble alto y ancho.
        out.write(byteArrayOf(GS, '!'.code.toByte(), if (big) 0x11 else 0x00))
    }

    private fun resetStyle(out: ByteArrayOutputStream) {
        setBold(out, false)
        setBig(out, false)
        setAlign(out, LineAlign.LEFT)
    }

    /** Parte un texto largo en varias lineas del ancho dado. */
    private fun wrap(text: String, width: Int): List<String> {
        if (text.isEmpty()) return listOf("")
        if (width <= 0) return listOf(text)
        val result = mutableListOf<String>()
        var remaining = text
        while (remaining.length > width) {
            var cut = remaining.lastIndexOf(' ', width)
            if (cut <= 0) cut = width
            result.add(remaining.substring(0, cut).trimEnd())
            remaining = remaining.substring(cut).trimStart()
        }
        result.add(remaining)
        return result
    }

    /** Quita acentos y caracteres no imprimibles para compatibilidad maxima. */
    private fun sanitize(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            sb.append(
                when (c) {
                    'á', 'à', 'ä', 'â', 'ã' -> 'a'
                    'é', 'è', 'ë', 'ê' -> 'e'
                    'í', 'ì', 'ï', 'î' -> 'i'
                    'ó', 'ò', 'ö', 'ô', 'õ' -> 'o'
                    'ú', 'ù', 'ü', 'û' -> 'u'
                    'Á', 'À', 'Ä', 'Â', 'Ã' -> 'A'
                    'É', 'È', 'Ë', 'Ê' -> 'E'
                    'Í', 'Ì', 'Ï', 'Î' -> 'I'
                    'Ó', 'Ò', 'Ö', 'Ô', 'Õ' -> 'O'
                    'Ú', 'Ù', 'Ü', 'Û' -> 'U'
                    'ñ' -> 'n'
                    'Ñ' -> 'N'
                    '¿' -> '?'
                    '¡' -> '!'
                    else -> if (c.code in 32..126) c else ' '
                }
            )
        }
        return sb.toString()
    }
}

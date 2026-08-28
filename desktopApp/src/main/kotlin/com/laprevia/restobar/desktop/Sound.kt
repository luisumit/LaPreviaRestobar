package com.laprevia.restobar.desktop

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import kotlin.math.PI
import kotlin.math.sin

/**
 * Campanita de "pedido nuevo" sintetizada en codigo (sin archivos de audio).
 * Dos tonos cortos ascendentes, en un hilo daemon para no bloquear la UI.
 */
object OrderSound {

    fun play() {
        Thread {
            runCatching {
                val rate = 44100f
                val format = AudioFormat(rate, 16, 1, true, false)
                val line = AudioSystem.getSourceDataLine(format)
                line.open(format)
                line.start()
                // (frecuencia Hz, duracion ms): La5 y Mi6 — un "ding-ding" agradable
                listOf(880.0 to 140, 1318.5 to 220).forEach { (freq, ms) ->
                    val samples = (rate * ms / 1000).toInt()
                    val buffer = ByteArray(samples * 2)
                    for (i in 0 until samples) {
                        val fadeOut = 1.0 - (i.toDouble() / samples) * 0.7
                        val value = (sin(2 * PI * freq * i / rate) * 32767 * 0.5 * fadeOut).toInt()
                        buffer[2 * i] = (value and 0xFF).toByte()
                        buffer[2 * i + 1] = ((value shr 8) and 0xFF).toByte()
                    }
                    line.write(buffer, 0, buffer.size)
                }
                line.drain()
                line.close()
            }
        }.apply { isDaemon = true }.start()
    }
}

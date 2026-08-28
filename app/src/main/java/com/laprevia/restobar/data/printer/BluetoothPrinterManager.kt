package com.laprevia.restobar.data.printer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Descubre impresoras Bluetooth emparejadas y les envia bytes ESC/POS por un
 * socket serie (SPP). No necesita internet.
 */
class BluetoothPrinterManager constructor(
    private val context: Context
) {
    companion object {
        // UUID estandar de Serial Port Profile (SPP) usado por las impresoras termicas.
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val adapter: BluetoothAdapter?
        get() = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    /** En Android 12+ hace falta el permiso runtime BLUETOOTH_CONNECT. */
    fun hasConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isBluetoothOn(): Boolean = adapter?.isEnabled == true

    /** Impresoras ya emparejadas en los ajustes del sistema. */
    fun pairedPrinters(): List<PrinterDevice> {
        if (!hasConnectPermission()) return emptyList()
        val bonded = try {
            adapter?.bondedDevices ?: emptySet()
        } catch (e: SecurityException) {
            emptySet()
        }
        return bonded.map { device ->
            val name = try {
                device.name ?: "Impresora"
            } catch (e: SecurityException) {
                "Impresora"
            }
            PrinterDevice(name = name, mac = device.address)
        }
    }

    /**
     * Envia los bytes a la impresora identificada por su MAC.
     * Devuelve [Result] para que la UI muestre exito o el motivo del fallo.
     */
    suspend fun printBytes(bytes: ByteArray, mac: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!hasConnectPermission()) {
            return@withContext Result.failure(IllegalStateException("Falta permiso de Bluetooth"))
        }
        val currentAdapter = adapter
            ?: return@withContext Result.failure(IllegalStateException("Este dispositivo no tiene Bluetooth"))
        if (!currentAdapter.isEnabled) {
            return@withContext Result.failure(IllegalStateException("Activa el Bluetooth"))
        }

        var socket: BluetoothSocket? = null
        try {
            val device = currentAdapter.getRemoteDevice(mac)
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            currentAdapter.cancelDiscovery()
            socket.connect()
            socket.outputStream.apply {
                write(bytes)
                flush()
            }
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(IllegalStateException("Permiso de Bluetooth denegado"))
        } catch (e: Exception) {
            Result.failure(IllegalStateException("No se pudo conectar con la impresora: ${e.message}"))
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {
            }
        }
    }
}

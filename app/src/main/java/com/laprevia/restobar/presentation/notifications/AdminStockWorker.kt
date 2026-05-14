package com.laprevia.restobar.presentation.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.laprevia.restobar.MainActivity
import com.laprevia.restobar.R
import androidx.hilt.work.HiltWorker  // ✅ AGREGAR ESTA LÍNEA (import)
import com.laprevia.restobar.data.local.db.AppDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
@HiltWorker  // ✅ AGREGAR ESTA LÍNEA (anotación)
class AdminStockWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted params: WorkerParameters,
    private val db: AppDatabase  // ✅ Inyectado por Hilt
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val products = db.productDao().getAll()

                val trackedProducts = products.filter { it.trackInventory }

                // Productos AGOTADOS (stock = 0)
                val outOfStock = trackedProducts.filter { it.stock == 0.0 }

                // Productos con STOCK BAJO (stock > 0 y stock <= mínimo)
                val lowStock = trackedProducts.filter { it.stock > 0 && it.stock <= it.minStock }

                if (outOfStock.isNotEmpty() || lowStock.isNotEmpty()) {
                    sendNotification(outOfStock, lowStock)
                    println("✅ Admin: Notificación enviada - ${outOfStock.size} agotados, ${lowStock.size} stock bajo")
                }

                Result.success()
            } catch (e: Exception) {
                println("❌ Admin: Error en worker: ${e.message}")
                e.printStackTrace()
                Result.retry()
            }
        }
    }

    private fun sendNotification(
        outOfStock: List<com.laprevia.restobar.data.local.entity.ProductEntity>,
        lowStock: List<com.laprevia.restobar.data.local.entity.ProductEntity>
    ) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "admin_stock_channel",
                "Alertas de Inventario",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de stock bajo y agotado para el administrador"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(appContext, MainActivity::class.java).apply {
            putExtra("open_inventory", true)
            putExtra("notification_type", "stock_alert")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            appContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir título
        val title = when {
            outOfStock.isNotEmpty() && lowStock.isNotEmpty() ->
                "⚠️ ${outOfStock.size} agotados + ${lowStock.size} stock bajo"
            outOfStock.isNotEmpty() ->
                "❌ ${outOfStock.size} producto(s) AGOTADOS"
            else ->
                "⚠️ ${lowStock.size} producto(s) con stock bajo"
        }

        // Construir mensaje
        val message = buildString {
            if (outOfStock.isNotEmpty()) {
                append("❌ AGOTADOS:\n")
                outOfStock.take(3).forEach { product ->
                    append("   • ${product.name}\n")
                }
                if (outOfStock.size > 3) {
                    append("   • y ${outOfStock.size - 3} más...\n")
                }
            }

            if (lowStock.isNotEmpty()) {
                if (outOfStock.isNotEmpty()) append("\n")
                append("⚠️ STOCK BAJO:\n")
                lowStock.take(3).forEach { product ->
                    append("   • ${product.name}: ${product.stock.toInt()} / ${product.minStock.toInt()} min\n")
                }
                if (lowStock.size > 3) {
                    append("   • y ${lowStock.size - 3} más...")
                }
            }
        }

        val color = if (outOfStock.isNotEmpty()) {
            appContext.getColor(R.color.notification_out_of_stock)
        } else {
            appContext.getColor(R.color.notification_low_stock)
        }

        val notification = NotificationCompat.Builder(appContext, "admin_stock_channel")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(color)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(2001, notification)
    }
}
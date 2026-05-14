package com.laprevia.restobar.presentation.notifications

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object AdminStockScheduler {

    private const val WORK_NAME = "admin_stock_worker"

    fun schedulePeriodicCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AdminStockWorker>(
            12, TimeUnit.HOURS,  // Cada 12 horas
            2, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        println("✅ AdminStockWorker programado (cada 12 horas)")
    }

    fun cancelPeriodicCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        println("❌ AdminStockWorker cancelado")
    }

    // Para pruebas inmediatas
    fun triggerImmediateCheck(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<AdminStockWorker>()
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
        println("🔄 Verificación de stock manual iniciada")
    }
}
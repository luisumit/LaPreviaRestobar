package com.laprevia.restobar.domain.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.laprevia.restobar.data.local.sync.SyncManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker  // ✅ IMPORTANTE: Esta anotación es necesaria
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncManager: SyncManager  // ✅ Se inyecta automáticamente
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                println("🔄 SyncWorker: Iniciando sincronización de fondo...")

                // Sincronizar datos pendientes
                syncManager.syncLight()  // Usar versión ligera para background

                println("✅ SyncWorker: Sincronización completada")
                Result.success()
            } catch (e: Exception) {
                println("❌ SyncWorker: Error - ${e.message}")

                // Reintentar si hay error (máximo 3 veces)
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }

    companion object {
        private const val WORK_NAME = "background_sync_work"
        private const val SYNC_INTERVAL_HOURS = 1L

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
                SYNC_INTERVAL_HOURS, TimeUnit.HOURS
            ).setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.MINUTES
                )
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )

            println("📅 SyncWorker: Programado cada $SYNC_INTERVAL_HOURS hora(s)")
        }

        fun scheduleImmediate(context: Context) {
            val immediateRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(immediateRequest)
            println("⚡ SyncWorker: Sincronización inmediata programada")
        }
    }
}
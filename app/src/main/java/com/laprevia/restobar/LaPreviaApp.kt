package com.laprevia.restobar

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.laprevia.restobar.domain.worker.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LaPreviaApp : Application() {  // ❌ Quitar Configuration.Provider

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // ✅ NO inicializar WorkManager manualmente
        // WorkManager ya se inicializa automáticamente con la configuración por defecto

        // Solo programar el worker (NO inicializar WorkManager)
        try {
            SyncWorker.schedule(this)
            println("✅ SyncWorker programado correctamente")
        } catch (e: Exception) {
            println("❌ Error programando SyncWorker: ${e.message}")
        }
    }
}
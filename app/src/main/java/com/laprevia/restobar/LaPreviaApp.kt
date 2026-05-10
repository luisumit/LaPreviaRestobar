package com.laprevia.restobar

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.laprevia.restobar.domain.worker.SyncWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class LaPreviaApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Timber solo en DEBUG
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        try {
            // ✅ Inicialización mínima de Firebase para evitar bloqueos
            FirebaseApp.initializeApp(this)
            Timber.i("🔥 FirebaseApp inicializado")
            
            // ⚠️ App Check desactivado temporalmente para pruebas locales
            // Evitamos que el bloqueo de red de App Check deje la pantalla negra
        } catch (e: Exception) {
            Timber.e(e, "❌ Error inicializando Firebase")
        }

        try {
            SyncWorker.schedule(this)
            Timber.d("✅ SyncWorker programado")
        } catch (e: Exception) {
            Timber.e(e, "❌ Error programando SyncWorker")
        }
    }
}

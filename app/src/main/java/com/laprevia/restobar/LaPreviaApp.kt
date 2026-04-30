package com.laprevia.restobar

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
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

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        try {
            SyncWorker.schedule(this)
            Timber.d("SyncWorker programado correctamente")
        } catch (e: Exception) {
            Timber.e(e, "Error programando SyncWorker")
        }
    }
}
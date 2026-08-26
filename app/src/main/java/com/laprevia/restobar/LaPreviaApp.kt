package com.laprevia.restobar

import android.app.Application
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.laprevia.restobar.di.koin.appKoinModules
import com.laprevia.restobar.domain.worker.SyncWorker
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class LaPreviaApp : Application(), Configuration.Provider {

    // WorkManager con inicializacion on-demand (el manifest deshabilita el initializer
    // automatico). Los workers son planos y resuelven sus dependencias via Koin.
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()

        // Timber solo en DEBUG
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Koin (Fase 3 de la migracion KMP): coexiste con Hilt hasta completar el switch.
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.WARNING else Level.ERROR)
            androidContext(this@LaPreviaApp)
            modules(appKoinModules)
        }

        try {
            // ✅ Inicialización mínima de Firebase para evitar bloqueos
            FirebaseApp.initializeApp(this)
            Timber.i("🔥 FirebaseApp inicializado")

            // Reporte remoto de crashes: activo en release, desactivado en debug
            FirebaseCrashlytics.getInstance().apply {
                setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
                setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
            }
            
            // App Check: protege tu backend de Firebase contra abuso externo.
            // - Debug usa el proveedor de debug (registra el token en la consola).
            // - Release usa Play Integrity.
            // Instalar el proveedor es seguro; la VERIFICACION (enforcement) se activa
            // en la consola de Firebase SOLO despues de confirmar que llegan tokens validos.
            try {
                val appCheck = FirebaseAppCheck.getInstance()
                if (BuildConfig.DEBUG) {
                    appCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                } else {
                    appCheck.installAppCheckProviderFactory(
                        PlayIntegrityAppCheckProviderFactory.getInstance()
                    )
                }
                Timber.i("🛡️ App Check inicializado")
            } catch (e: Exception) {
                Timber.e(e, "❌ Error inicializando App Check")
            }
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

package com.laprevia.restobar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.laprevia.restobar.presentation.navigation.AppNavigation
import com.laprevia.restobar.presentation.theme.LaPreviaRestoBarTheme
import com.laprevia.restobar.domain.service.FirebaseInitializerService
import com.laprevia.restobar.domain.service.InventorySyncService
import com.laprevia.restobar.data.local.sync.SyncManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var firebaseInitializerService: FirebaseInitializerService

    @Inject
    lateinit var inventorySyncService: InventorySyncService

    @Inject
    lateinit var syncManager: SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            LaPreviaRestoBarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }

        lifecycleScope.launch {
            try {
                Timber.i("🚀 MainActivity: Iniciando aplicación con Firebase...")
                firebaseInitializerService.initializeAllData()

                Timber.i("🔄 MainActivity: Iniciando sincronización de inventario...")
                inventorySyncService.startInventorySync()

                Timber.i("🔄 MainActivity: Sincronizando datos offline...")
                syncManager.syncFull()

                delay(2000)
                firebaseInitializerService.checkFirebaseStatus()

                Timber.i("✅ MainActivity: Todo inicializado correctamente")

            } catch (e: Exception) {
                Timber.e(e, "💥 MainActivity: Error")
            }
        }
    }
}

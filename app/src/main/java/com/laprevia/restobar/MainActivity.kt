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

        lifecycleScope.launch {
            try {
                println("🚀 MainActivity: Iniciando aplicación con Firebase...")
                firebaseInitializerService.initializeAllData()

                println("🔄 MainActivity: Iniciando sincronización de inventario...")
                inventorySyncService.startInventorySync()

                // ✅ SYNC OFFLINE → FIREBASE (CORREGIDO: syncFull en lugar de syncAll)
                println("🔄 MainActivity: Sincronizando datos offline...")
                syncManager.syncFull()  // ← CAMBIADO DE syncAll() A syncFull()

                // Esperar un poco y verificar el estado
                delay(2000)
                firebaseInitializerService.checkFirebaseStatus()

                println("✅ MainActivity: Todo inicializado correctamente")

            } catch (e: Exception) {
                println("💥 MainActivity: Error - ${e.message}")
                e.printStackTrace()
            }
        }

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
    }
}
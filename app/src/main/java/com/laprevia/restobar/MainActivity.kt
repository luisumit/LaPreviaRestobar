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

        // ✅ PRIORIDAD MÁXIMA: Dibujar la interfaz inmediatamente
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

        // ✅ INICIALIZACIÓN DIFERIDA: No bloquea el dibujo de la pantalla
        lifecycleScope.launch {
            try {
                // Esperamos un momento para que la UI se asiente
                delay(1000)
                
                Timber.i("🚀 MainActivity: Iniciando servicios de fondo...")
                
                // Intentar inicializar, pero capturar errores para que no afecten a la UI
                launch { 
                    try { firebaseInitializerService.initializeAllData() } 
                    catch (e: Exception) { Timber.w("⚠️ Fallo inicialización datos") }
                }
                
                launch { 
                    try { inventorySyncService.startInventorySync() } 
                    catch (e: Exception) { Timber.w("⚠️ Fallo sync inventario") }
                }

                launch { 
                    try { syncManager.syncFull() } 
                    catch (e: Exception) { Timber.w("⚠️ Fallo sync full") }
                }

            } catch (e: Exception) {
                Timber.e(e, "💥 MainActivity: Error en procesos de fondo")
            }
        }
    }
}

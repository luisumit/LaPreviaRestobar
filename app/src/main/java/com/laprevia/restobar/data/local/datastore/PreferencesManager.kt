package com.laprevia.restobar.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.laprevia.restobar.data.model.UserRole
import com.laprevia.restobar.data.printer.PaperWidth
import com.laprevia.restobar.data.printer.PrinterConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// ✅ CORREGIDO: Usa el mismo nombre que en el otro archivo
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {

    private object PreferencesKeys {
        val USER_ROLE = stringPreferencesKey("user_role")
        val PRINTER_MAC = stringPreferencesKey("printer_mac")
        val PRINTER_NAME = stringPreferencesKey("printer_name")
        val PRINTER_PAPER = stringPreferencesKey("printer_paper")
        val AUTO_PRINT_COMANDA = booleanPreferencesKey("auto_print_comanda")
        val AUTO_PRINT_TICKET = booleanPreferencesKey("auto_print_ticket")
        val WHATSAPP_NUMBER = stringPreferencesKey("whatsapp_number")
    }

    val whatsappNumber: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.WHATSAPP_NUMBER] ?: ""
    }

    suspend fun saveWhatsappNumber(number: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WHATSAPP_NUMBER] = number
        }
    }

    suspend fun saveUserRole(role: UserRole) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_ROLE] = when (role) {
                is UserRole.MESERO -> "MESERO"
                is UserRole.COCINERO -> "COCINERO"
                is UserRole.ADMIN -> "ADMIN"
            }
        }
    }

    val userRole: Flow<UserRole?> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USER_ROLE]?.let { roleName ->
            UserRole.fromString(roleName)
        }
    }

    // ---- Configuracion de impresora termica ----

    val printerConfig: Flow<PrinterConfig> = context.dataStore.data.map { preferences ->
        PrinterConfig(
            mac = preferences[PreferencesKeys.PRINTER_MAC],
            name = preferences[PreferencesKeys.PRINTER_NAME],
            paperWidth = PaperWidth.fromString(preferences[PreferencesKeys.PRINTER_PAPER]),
            autoPrintComanda = preferences[PreferencesKeys.AUTO_PRINT_COMANDA] ?: false,
            autoPrintTicket = preferences[PreferencesKeys.AUTO_PRINT_TICKET] ?: false
        )
    }

    suspend fun savePrinter(mac: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRINTER_MAC] = mac
            preferences[PreferencesKeys.PRINTER_NAME] = name
        }
    }

    suspend fun savePaperWidth(paperWidth: PaperWidth) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PRINTER_PAPER] = paperWidth.name
        }
    }

    suspend fun setAutoPrintComanda(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_PRINT_COMANDA] = enabled
        }
    }

    suspend fun setAutoPrintTicket(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_PRINT_TICKET] = enabled
        }
    }

    suspend fun clearUserData() {
        context.dataStore.edit { preferences ->
            preferences.clear() // ✅ Mejor usar clear() en lugar de remove individual
        }
    }
}

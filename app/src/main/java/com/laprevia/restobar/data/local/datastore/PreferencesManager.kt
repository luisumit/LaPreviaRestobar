package com.laprevia.restobar.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.laprevia.restobar.data.model.UserRole
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

    suspend fun clearUserData() {
        context.dataStore.edit { preferences ->
            preferences.clear() // ✅ Mejor usar clear() en lugar de remove individual
        }
    }
}

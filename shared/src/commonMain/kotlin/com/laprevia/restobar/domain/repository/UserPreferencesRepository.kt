package com.laprevia.restobar.domain.repository

import com.laprevia.restobar.data.model.UserRole
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val userRole: Flow<UserRole?>
    suspend fun saveUserRole(role: UserRole)
    suspend fun clearUserData()
}

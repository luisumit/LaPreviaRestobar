package com.laprevia.restobar.data.repository

import com.laprevia.restobar.data.local.datastore.PreferencesManager
import com.laprevia.restobar.data.model.UserRole
import com.laprevia.restobar.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val preferencesManager: PreferencesManager
) : UserPreferencesRepository {

    override val userRole: Flow<UserRole?>
        get() = preferencesManager.userRole

    override suspend fun saveUserRole(role: UserRole) {
        preferencesManager.saveUserRole(role)
    }

    override suspend fun clearUserData() {
        preferencesManager.clearUserData()
    }
}
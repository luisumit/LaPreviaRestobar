package com.laprevia.restobar.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laprevia.restobar.data.local.sync.SyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncManager: SyncManager
) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    fun syncManually() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing

            try {
                syncManager.syncFull()
                _syncState.value = SyncState.Success
                _lastSyncTime.value = System.currentTimeMillis()
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun syncUpload() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            try {
                syncManager.uploadAll()
                _syncState.value = SyncState.Success
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Upload failed")
            }
        }
    }

    fun syncDownload() {
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            try {
                syncManager.downloadAll()
                _syncState.value = SyncState.Success
            } catch (e: Exception) {
                _syncState.value = SyncState.Error(e.message ?: "Download failed")
            }
        }
    }

    sealed class SyncState {
        object Idle : SyncState()
        object Syncing : SyncState()
        object Success : SyncState()
        data class Error(val message: String) : SyncState()
    }
}
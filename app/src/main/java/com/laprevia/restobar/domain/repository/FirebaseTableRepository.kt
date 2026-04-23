package com.laprevia.restobar.domain.repository

import com.laprevia.restobar.data.model.Table
import kotlinx.coroutines.flow.Flow

interface FirebaseTableRepository : TableRepository {
    // Métodos específicos para Firebase (tiempo real)
    fun listenToTableChanges(): Flow<Table>
    fun getTablesRealTime(): Flow<List<Table>>
}
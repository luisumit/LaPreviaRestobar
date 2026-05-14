package com.laprevia.restobar.domain.repository

import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.data.model.TableStatus
import kotlinx.coroutines.flow.Flow

interface TableRepository {

    // ==================== LISTADO ====================
    fun getTables(): Flow<List<Table>>

    // ==================== BÚSQUEDA ====================
    suspend fun getTableById(tableId: Int): Table?
    suspend fun getTablesCount(): Int

    // ==================== ESTADOS ====================
    suspend fun updateTableStatus(tableId: Int, status: TableStatus)
    suspend fun assignOrderToTable(tableId: Int, orderId: String)
    suspend fun clearTable(tableId: Int)
    suspend fun updateTable(table: Table)

    // ==================== INICIALIZACIÓN ====================
    suspend fun initializeDefaultTables()

    // ==================== DEBUG (DESARROLLO) ====================
    suspend fun debugTables(): String

    // ==================== OFFLINE SYNC (CLAVE) ====================

    /**
     * Sincroniza mesas pendientes con Firebase
     */
    suspend fun syncPendingTables()

    /**
     * Obtiene mesas no sincronizadas (Room)
     */
    fun getPendingTables(): Flow<List<Table>>
}

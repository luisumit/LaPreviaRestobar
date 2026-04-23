package com.laprevia.restobar.domain.repository

import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.data.model.TableStatus
import kotlinx.coroutines.flow.Flow

interface TableRepository {
    fun getTables(): Flow<List<Table>>
    suspend fun updateTableStatus(tableId: Int, status: TableStatus)
    suspend fun assignOrderToTable(tableId: Int, orderId: String)
    suspend fun clearTable(tableId: Int)
    suspend fun getTableById(tableId: Int): Table?
    suspend fun updateTable(table: Table)
    // ✅ NUEVOS MÉTODOS PARA INICIALIZACIÓN
    suspend fun initializeDefaultTables()
    suspend fun getTablesCount(): Int
    suspend fun debugTables(): String
}

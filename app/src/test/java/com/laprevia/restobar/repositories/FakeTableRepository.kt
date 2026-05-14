package com.laprevia.restobar.repositories

import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.data.model.TableStatus
import com.laprevia.restobar.domain.repository.FirebaseTableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeTableRepository : FirebaseTableRepository {
    private val tables = mutableListOf<Table>()

    override fun listenToTableChanges(): Flow<Table> = flowOf()
    override fun getTablesRealTime(): Flow<List<Table>> = flowOf(tables)
    override fun getTables(): Flow<List<Table>> = flowOf(tables)

    override suspend fun getTableById(tableId: Int): Table? {
        return tables.find { it.id == tableId }
    }

    override suspend fun getTablesCount(): Int = tables.size

    override suspend fun updateTableStatus(tableId: Int, status: TableStatus) {
        val index = tables.indexOfFirst { it.id == tableId }
        if (index != -1) tables[index] = tables[index].copy(status = status)
    }

    override suspend fun assignOrderToTable(tableId: Int, orderId: String) {
        val index = tables.indexOfFirst { it.id == tableId }
        if (index != -1) {
            tables[index] = tables[index].copy(
                status = TableStatus.OCUPADA,
                currentOrderId = orderId
            )
        }
    }

    override suspend fun clearTable(tableId: Int) {
        val index = tables.indexOfFirst { it.id == tableId }
        if (index != -1) {
            tables[index] = tables[index].copy(
                status = TableStatus.LIBRE,
                currentOrderId = null
            )
        }
    }

    override suspend fun updateTable(table: Table) {
        val index = tables.indexOfFirst { it.id == table.id }
        if (index != -1) tables[index] = table
    }

    override suspend fun initializeDefaultTables() {
        for (i in 1..8) {
            tables.add(
                Table(
                    id = i,
                    number = i,
                    status = TableStatus.LIBRE,
                    currentOrderId = null
                )
            )
        }
    }

    override suspend fun debugTables(): String = "debug: ${tables.size} tables"

    override suspend fun syncPendingTables() {}

    override fun getPendingTables(): Flow<List<Table>> = flowOf(emptyList())
}
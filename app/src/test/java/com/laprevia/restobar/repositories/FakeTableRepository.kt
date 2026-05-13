package com.laprevia.restobar.repositories

import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.data.model.TableStatus
import com.laprevia.restobar.domain.repository.TableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeTableRepository : TableRepository {
    private val tables = mutableListOf<Table>()

    init {
        // Agregar mesas de ejemplo
        for (i in 1..10) {
            tables.add(Table(i, i, TableStatus.LIBRE))
        }
    }

    override fun getTables(): Flow<List<Table>> = flow {
        emit(tables)
    }

    override suspend fun getTableById(tableId: Int): Table? {
        return tables.find { it.id == tableId }
    }

    override suspend fun getTablesCount(): Int = tables.size

    override suspend fun updateTableStatus(tableId: Int, status: TableStatus) {
        val index = tables.indexOfFirst { it.id == tableId }
        if (index != -1) {
            tables[index] = tables[index].copy(status = status)
        }
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
        if (index != -1) {
            tables[index] = table
        }
    }

    override suspend fun initializeDefaultTables() {
        // No-op for fake
    }

    override suspend fun debugTables(): String = "Fake debug info"

    override suspend fun syncPendingTables() {
        // No-op for fake
    }

    override fun getPendingTables(): Flow<List<Table>> = flow {
        emit(emptyList())
    }
}

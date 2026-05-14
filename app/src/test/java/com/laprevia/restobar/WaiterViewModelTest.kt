package com.laprevia.restobar

import com.laprevia.restobar.data.model.TableStatus
import com.laprevia.restobar.repositories.FakeTableRepository
import com.laprevia.restobar.repositories.FakeOrderRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TableRepositoryTest {

    @Test
    fun `crear orden deberia cambiar estado de mesa a OCUPADA`() = runTest {
        val tableRepo = FakeTableRepository()
        tableRepo.initializeDefaultTables()

        tableRepo.assignOrderToTable(tableId = 1, orderId = "orden-123")

        val table = tableRepo.getTableById(1)
        assertEquals(TableStatus.OCUPADA, table?.status)
    }

    @Test
    fun `liberar mesa deberia cambiar estado a LIBRE`() = runTest {
        val tableRepo = FakeTableRepository()
        tableRepo.initializeDefaultTables()
        tableRepo.assignOrderToTable(tableId = 1, orderId = "orden-123")

        tableRepo.clearTable(tableId = 1)

        val table = tableRepo.getTableById(1)
        assertEquals(TableStatus.LIBRE, table?.status)
    }
}
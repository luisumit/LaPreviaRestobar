package com.laprevia.restobar


import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WaiterViewModelTest {

    private lateinit var viewModel: com.laprevia.restobar.presentation.viewmodel.WaiterViewModel
    private lateinit var fakeTableRepository: FakeTableRepository
    private lateinit var fakeOrderRepository: FakeOrderRepository
    private lateinit var fakeProductRepository: FakeProductRepository

    @Before
    fun setup() {
        fakeTableRepository = FakeTableRepository()
        fakeOrderRepository = FakeOrderRepository()
        fakeProductRepository = FakeProductRepository()
        viewModel = com.laprevia.restobar.presentation.viewmodel.WaiterViewModel(
            fakeTableRepository,
            fakeOrderRepository,
            fakeProductRepository
        )
    }

    @Test
    fun `create order should update table status`() = runTest {
        // Given
        val tableId = 1
        val items = listOf(
            com.laprevia.restobar.data.model.OrderItem(
                com.laprevia.restobar.data.model.Product("1", "Cerveza", 5.0, "Bebidas", 10),
                2
            )
        )

        // When
        viewModel.createOrder(tableId, items)

        // Then
        val table = fakeTableRepository.getTableById(tableId)
        assertEquals(com.laprevia.restobar.data.model.TableStatus.OCUPADA, table?.status)
    }
}

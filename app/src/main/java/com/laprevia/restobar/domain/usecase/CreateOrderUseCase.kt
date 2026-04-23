
package com.laprevia.restobar.domain.usecase

import com.laprevia.restobar.data.model.Order
import com.laprevia.restobar.domain.repository.OrderRepository
import com.laprevia.restobar.domain.repository.TableRepository
import javax.inject.Inject

class CreateOrderUseCase @Inject constructor(
    private val orderRepository: OrderRepository,
    private val tableRepository: TableRepository
) {
    suspend operator fun invoke(order: Order) {
        orderRepository.createOrder(order)
        tableRepository.assignOrderToTable(order.tableId, order.id)
    }
}

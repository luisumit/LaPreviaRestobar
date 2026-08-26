package com.laprevia.restobar.domain.usecase

import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.domain.repository.OrderRepository

class UpdateOrderStatusUseCase constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: String, status: OrderStatus) {
        orderRepository.updateOrderStatus(orderId, status.name)
    }
}

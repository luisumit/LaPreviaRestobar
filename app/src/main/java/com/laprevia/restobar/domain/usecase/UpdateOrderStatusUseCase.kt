package com.laprevia.restobar.domain.usecase

import com.laprevia.restobar.data.model.OrderStatus
import com.laprevia.restobar.domain.repository.OrderRepository
import javax.inject.Inject

class UpdateOrderStatusUseCase @Inject constructor(
    private val orderRepository: OrderRepository
) {
    suspend operator fun invoke(orderId: String, status: OrderStatus) {
        orderRepository.updateOrderStatus(orderId, status.name)
    }
}
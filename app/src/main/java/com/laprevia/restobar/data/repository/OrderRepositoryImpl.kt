package com.laprevia.restobar.data.repository

import com.laprevia.restobar.data.local.dao.OrderDao
import com.laprevia.restobar.data.local.entity.OrderEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val orderDao: OrderDao
) {

    suspend fun createOrder(order: OrderEntity) {
        orderDao.insert(order.copy(syncStatus = "PENDING"))
    }

    suspend fun getAll() = orderDao.getAll()

    suspend fun getPending() = orderDao.getPending()

    suspend fun updateStatus(id: String, status: String) {
        orderDao.updateStatus(id, status)
    }
}

package com.laprevia.restobar.data.repository

import com.laprevia.restobar.data.local.dao.ProductDao
import com.laprevia.restobar.data.local.entity.ProductEntity

class ProductRepositoryImpl constructor(
    private val productDao: ProductDao
) {

    suspend fun createProduct(product: ProductEntity) {
        productDao.insert(product.copy(syncStatus = "PENDING"))
    }

    suspend fun getAll() = productDao.getAll()

    suspend fun getPending() = productDao.getPending()

    suspend fun updateStatus(id: String, status: String) {
        productDao.updateStatus(id, status)
    }
}

package com.laprevia.restobar.data.repository

import com.laprevia.restobar.data.local.dao.TableDao
import com.laprevia.restobar.data.local.entity.TableEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TableRepositoryImpl @Inject constructor(
    private val tableDao: TableDao
) {

    suspend fun updateTable(table: TableEntity) {
        tableDao.insert(table.copy(syncStatus = "PENDING"))
    }

    suspend fun getAll() = tableDao.getAll()

    suspend fun getPending() = tableDao.getPending()

    suspend fun updateStatus(id: Int, status: String) {
        tableDao.updateStatus(id, status)
    }
}
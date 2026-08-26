
package com.laprevia.restobar.domain.usecase

import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.domain.repository.TableRepository
import kotlinx.coroutines.flow.Flow

class GetTablesUseCase constructor(
    private val tableRepository: TableRepository
) {
    operator fun invoke(): Flow<List<Table>> {
        return tableRepository.getTables()
    }
}

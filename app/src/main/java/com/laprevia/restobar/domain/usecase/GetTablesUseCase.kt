
package com.laprevia.restobar.domain.usecase

import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.domain.repository.TableRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTablesUseCase @Inject constructor(
    private val tableRepository: TableRepository
) {
    operator fun invoke(): Flow<List<Table>> {
        return tableRepository.getTables()
    }
}

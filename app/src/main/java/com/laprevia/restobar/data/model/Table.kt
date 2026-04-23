// Table.kt - AGREGA EL CAMPO CAPACITY
package com.laprevia.restobar.data.model

data class Table(
    val id: Int,
    val number: Int,
    val status: TableStatus,
    val currentOrderId: String? = null,
    val capacity: Int = 4 // ✅ AGREGADO
)

enum class TableStatus {
    LIBRE, OCUPADA, RESERVADA // ✅ AGREGADO RESERVADA
}
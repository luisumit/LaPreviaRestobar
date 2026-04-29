package com.laprevia.restobar.data.model

data class Table(
    val id: Int,
    val number: Int,
    val status: TableStatus,
    val currentOrderId: String? = null,
    val capacity: Int = 4,
    val version: Long = 0,
    val syncStatus: String = "SYNCED"  // ✅ AGREGAR ESTO
) {
    // Constructor sin argumentos para Firebase
    constructor() : this(
        id = 0,
        number = 0,
        status = TableStatus.LIBRE,
        currentOrderId = null,
        capacity = 4,
        version = 0,
        syncStatus = "SYNCED"
    )
}

enum class TableStatus {
    LIBRE,
    OCUPADA,
    RESERVADA
}
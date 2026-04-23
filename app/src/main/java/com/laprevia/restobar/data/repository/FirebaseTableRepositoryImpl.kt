// data/repository/FirebaseTableRepositoryImpl.kt
package com.laprevia.restobar.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.data.model.TableStatus
import com.laprevia.restobar.domain.repository.FirebaseTableRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseTableRepositoryImpl @Inject constructor(
    private val tablesRef: DatabaseReference
) : FirebaseTableRepository {

    // ==================== MÉTODOS OBLIGATORIOS DE TableRepository ====================

    override fun getTables(): Flow<List<Table>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tables = snapshot.children.mapNotNull { it.toTable() }
                println("🔥 FirebaseTables: ${tables.size} mesas cargadas")
                trySend(tables)
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ FirebaseTables: Error en getTables: ${error.message}")
                close(error.toException())
            }
        }

        tablesRef.addValueEventListener(eventListener)
        awaitClose { tablesRef.removeEventListener(eventListener) }
    }

    override suspend fun updateTableStatus(tableId: Int, status: TableStatus) {
        try {
            println("🔄 FirebaseTables: Actualizando estado mesa $tableId a $status")

            val updates = mapOf(
                "status" to status.name
            )
            tablesRef.child(tableId.toString()).updateChildren(updates).await()

            println("✅ FirebaseTables: Estado de mesa actualizado exitosamente")
        } catch (e: Exception) {
            println("❌ FirebaseTables: Error actualizando estado de mesa: ${e.message}")
            throw e
        }
    }

    override suspend fun assignOrderToTable(tableId: Int, orderId: String) {
        try {
            println("📝 FirebaseTables: Asignando orden $orderId a mesa $tableId")

            val updates = mapOf(
                "status" to TableStatus.OCUPADA.name,
                "currentOrderId" to orderId
            )
            tablesRef.child(tableId.toString()).updateChildren(updates).await()

            println("✅ FirebaseTables: Orden asignada a mesa exitosamente")
        } catch (e: Exception) {
            println("❌ FirebaseTables: Error asignando orden a mesa: ${e.message}")
            throw e
        }
    }

    override suspend fun clearTable(tableId: Int) {
        try {
            println("🧹 FirebaseTables: Limpiando mesa $tableId")

            val updates = mapOf(
                "status" to TableStatus.LIBRE.name,
                "currentOrderId" to null
            )
            tablesRef.child(tableId.toString()).updateChildren(updates).await()

            println("✅ FirebaseTables: Mesa limpiada exitosamente")
        } catch (e: Exception) {
            println("❌ FirebaseTables: Error limpiando mesa: ${e.message}")
            throw e
        }
    }

    override suspend fun getTableById(tableId: Int): Table? {
        return try {
            println("🔍 FirebaseTables: Buscando mesa por ID: $tableId")
            val snapshot = tablesRef.child(tableId.toString()).get().await()
            val table = snapshot.toTable()
            if (table != null) {
                println("✅ FirebaseTables: Mesa encontrada: ${table.number}")
            } else {
                println("❌ FirebaseTables: Mesa no encontrada: $tableId")
            }
            table
        } catch (e: Exception) {
            println("❌ FirebaseTables: Error buscando mesa $tableId: ${e.message}")
            null
        }
    }

    override suspend fun updateTable(table: Table) {
        try {
            println("🔄 FirebaseTables: Actualizando mesa: ${table.number}")

            val tableMap = table.toFirebaseMap()
            tablesRef.child(table.id.toString()).updateChildren(tableMap).await()

            println("✅ FirebaseTables: Mesa actualizada exitosamente")
        } catch (e: Exception) {
            println("❌ FirebaseTables: Error actualizando mesa: ${e.message}")
            throw e
        }
    }

    // ==================== MÉTODOS DE INICIALIZACIÓN ====================

    override suspend fun initializeDefaultTables() {
        try {
            println("🔄 FirebaseTables: Inicializando mesas por defecto...")

            // Verificar si ya existen mesas
            val snapshot = tablesRef.get().await()
            if (!snapshot.exists() || snapshot.children.count() == 0) {
                val defaultTables = listOf(
                    Table(1, 1, TableStatus.LIBRE, capacity = 4),
                    Table(2, 2, TableStatus.LIBRE, capacity = 4),
                    Table(3, 3, TableStatus.LIBRE, capacity = 6),
                    Table(4, 4, TableStatus.LIBRE, capacity = 6),
                    Table(5, 5, TableStatus.LIBRE, capacity = 2),
                    Table(6, 6, TableStatus.LIBRE, capacity = 2),
                    Table(7, 7, TableStatus.LIBRE, capacity = 8),
                    Table(8, 8, TableStatus.LIBRE, capacity = 4)
                )

                println("🆕 FirebaseTables: Creando ${defaultTables.size} mesas por defecto")

                defaultTables.forEach { table ->
                    tablesRef.child(table.id.toString()).setValue(table.toFirebaseMap()).await()
                }

                println("✅ FirebaseTables: Mesas inicializadas exitosamente")
            } else {
                println("ℹ️ FirebaseTables: Las mesas ya existen, omitiendo inicialización")
            }

        } catch (e: Exception) {
            println("❌ FirebaseTables: Error inicializando mesas: ${e.message}")
            throw e
        }
    }

    override suspend fun getTablesCount(): Int {
        return try {
            val snapshot = tablesRef.get().await()
            val count = snapshot.childrenCount.toInt()
            println("📊 FirebaseTables: Total de mesas: $count")
            count
        } catch (e: Exception) {
            println("❌ FirebaseTables: Error obteniendo conteo de mesas: ${e.message}")
            0
        }
    }

    override suspend fun debugTables(): String {
        return try {
            val snapshot = tablesRef.get().await()
            val tables = snapshot.children.mapNotNull { it.toTable() }

            val debugInfo = buildString {
                append("Mesas en Firebase: ${tables.size}\n")
                tables.forEach { table ->
                    append("Mesa ${table.number}: ${table.status}")
                    table.currentOrderId?.let { orderId ->
                        append(" (Orden: ${orderId.take(8)}...)")
                    }
                    appendLine()
                }
            }

            println("🐛 FirebaseTables: Debug info generada")
            debugInfo
        } catch (e: Exception) {
            val errorMsg = "Error: ${e.message}"
            println("❌ FirebaseTables: $errorMsg")
            errorMsg
        }
    }

    // ==================== MÉTODOS ESPECÍFICOS DE FIREBASE ====================

    override fun listenToTableChanges(): Flow<Table> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    child.toTable()?.let { table ->
                        println("📡 FirebaseTables: Cambio detectado en mesa ${table.number}")
                        trySend(table)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                println("❌ FirebaseTables: Error en listenToTableChanges: ${error.message}")
                close(error.toException())
            }
        }

        tablesRef.addValueEventListener(eventListener)
        awaitClose { tablesRef.removeEventListener(eventListener) }
    }

    override fun getTablesRealTime(): Flow<List<Table>> = getTables()

    // ==================== MÉTODOS UTILITARIOS ====================

    /**
     * Convierte un DataSnapshot de Firebase a un objeto Table
     */
    private fun DataSnapshot.toTable(): Table? {
        return try {
            val id = key?.toIntOrNull() ?: return null
            val number = child("number").getValue(Int::class.java) ?: 0
            val statusStr = child("status").getValue(String::class.java) ?: "LIBRE"
            val status = TableStatus.valueOf(statusStr)
            val currentOrderId = child("currentOrderId").getValue(String::class.java)
            val capacity = child("capacity").getValue(Int::class.java) ?: 4

            Table(
                id = id,
                number = number,
                status = status,
                currentOrderId = currentOrderId,
                capacity = capacity
            )
        } catch (e: Exception) {
            println("❌ FirebaseTables: Error convirtiendo DataSnapshot: ${e.message}")
            null
        }
    }

    /**
     * Convierte un objeto Table a un Map para Firebase
     */
    private fun Table.toFirebaseMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "number" to number,
            "status" to status.name,
            "currentOrderId" to currentOrderId,
            "capacity" to capacity
        )
    }

    // ==================== MÉTODOS ADICIONALES UTILES ====================

    /**
     * Obtiene mesas por estado
     */
    suspend fun getTablesByStatus(status: TableStatus): List<Table> {
        return try {
            println("🔍 FirebaseTables: Buscando mesas con estado: $status")
            val snapshot = tablesRef.orderByChild("status").equalTo(status.name).get().await()
            val tables = snapshot.children.mapNotNull { it.toTable() }
            println("✅ FirebaseTables: ${tables.size} mesas encontradas con estado $status")
            tables
        } catch (e: Exception) {
            println("❌ FirebaseTables: Error obteniendo mesas por estado: ${e.message}")
            emptyList()
        }
    }

    /**
     * Obtiene mesas disponibles (libres)
     */
    suspend fun getAvailableTables(): List<Table> {
        return getTablesByStatus(TableStatus.LIBRE)
    }

    /**
     * Obtiene mesas ocupadas
     */
    suspend fun getOccupiedTables(): List<Table> {
        return getTablesByStatus(TableStatus.OCUPADA)
    }

    /**
     * Reserva una mesa
     */
    suspend fun reserveTable(tableId: Int) {
        try {
            println("📅 FirebaseTables: Reservando mesa $tableId")

            val updates = mapOf(
                "status" to TableStatus.RESERVADA.name
            )
            tablesRef.child(tableId.toString()).updateChildren(updates).await()

            println("✅ FirebaseTables: Mesa reservada exitosamente")
        } catch (e: Exception) {
            println("❌ FirebaseTables: Error reservando mesa: ${e.message}")
            throw e
        }
    }

    /**
     * Obtiene estadísticas de mesas
     */
    suspend fun getTableStats(): Map<String, Any> {
        return try {
            val snapshot = tablesRef.get().await()
            val tables = snapshot.children.mapNotNull { it.toTable() }

            val totalTables = tables.size
            val freeTables = tables.count { it.status == TableStatus.LIBRE }
            val occupiedTables = tables.count { it.status == TableStatus.OCUPADA }
            val reservedTables = tables.count { it.status == TableStatus.RESERVADA }
            val totalCapacity = tables.sumOf { it.capacity }

            mapOf(
                "totalTables" to totalTables,
                "freeTables" to freeTables,
                "occupiedTables" to occupiedTables,
                "reservedTables" to reservedTables,
                "totalCapacity" to totalCapacity
            )
        } catch (e: Exception) {
            println("❌ FirebaseTables: Error obteniendo estadísticas: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Verifica si una mesa existe
     */
    suspend fun tableExists(tableId: Int): Boolean {
        return try {
            val snapshot = tablesRef.child(tableId.toString()).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            println("❌ FirebaseTables: Error verificando existencia de mesa: ${e.message}")
            false
        }
    }

    /**
     * Obtiene la mesa por número (no ID)
     */
    suspend fun getTableByNumber(tableNumber: Int): Table? {
        return try {
            println("🔍 FirebaseTables: Buscando mesa por número: $tableNumber")
            val snapshot = tablesRef.orderByChild("number").equalTo(tableNumber.toDouble()).get().await()
            val table = snapshot.children.firstOrNull()?.toTable()
            if (table != null) {
                println("✅ FirebaseTables: Mesa encontrada: ${table.number}")
            } else {
                println("❌ FirebaseTables: Mesa no encontrada con número: $tableNumber")
            }
            table
        } catch (e: Exception) {
            println("❌ FirebaseTables: Error buscando mesa por número: ${e.message}")
            null
        }
    }
}
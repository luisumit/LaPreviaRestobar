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
import com.laprevia.restobar.di.TablesReference

@Singleton
class FirebaseTableRepositoryImpl @Inject constructor(
    @TablesReference
    private val tablesRef: DatabaseReference
) : FirebaseTableRepository {

    // ==================== MÉTODOS OBLIGATORIOS DE TableRepository ====================

    override fun getTables(): Flow<List<Table>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tables = snapshot.children.mapNotNull { it.toTable() }
                timber.log.Timber.d("🔥 FirebaseTables: ${tables.size} mesas cargadas")
                trySend(tables)
            }

            override fun onCancelled(error: DatabaseError) {
                timber.log.Timber.d("❌ FirebaseTables: Error en getTables: ${error.message}")
                close(error.toException())
            }
        }

        tablesRef.addValueEventListener(eventListener)
        awaitClose { tablesRef.removeEventListener(eventListener) }
    }

    // ✅ NUEVO MÉTODO: getPendingTables
    override fun getPendingTables(): Flow<List<Table>> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tables = snapshot.children.mapNotNull { it.toTable() }
                    .filter { it.syncStatus == "PENDING" }
                timber.log.Timber.d("⏳ FirebaseTables: ${tables.size} mesas pendientes")
                trySend(tables)
            }

            override fun onCancelled(error: DatabaseError) {
                timber.log.Timber.d("❌ FirebaseTables: Error en getPendingTables: ${error.message}")
                close(error.toException())
            }
        }

        tablesRef.orderByChild("syncStatus").equalTo("PENDING")
            .addValueEventListener(eventListener)

        awaitClose { tablesRef.removeEventListener(eventListener) }
    }

    override suspend fun updateTableStatus(tableId: Int, status: TableStatus) {
        try {
            timber.log.Timber.d("🔄 FirebaseTables: Actualizando estado mesa $tableId a $status")

            val updates = mapOf(
                "status" to status.name,
                "updatedAt" to System.currentTimeMillis()
            )
            tablesRef.child(tableId.toString()).updateChildren(updates).await()

            timber.log.Timber.d("✅ FirebaseTables: Estado de mesa actualizado exitosamente")
        } catch (e: Exception) {
            timber.log.Timber.d("❌ FirebaseTables: Error actualizando estado de mesa: ${e.message}")
            throw e
        }
    }

    override suspend fun assignOrderToTable(tableId: Int, orderId: String) {
        try {
            timber.log.Timber.d("📝 FirebaseTables: Asignando orden $orderId a mesa $tableId")

            val updates = mapOf(
                "status" to TableStatus.OCUPADA.name,
                "currentOrderId" to orderId,
                "updatedAt" to System.currentTimeMillis()
            )
            tablesRef.child(tableId.toString()).updateChildren(updates).await()

            timber.log.Timber.d("✅ FirebaseTables: Orden asignada a mesa exitosamente")
        } catch (e: Exception) {
            timber.log.Timber.d("❌ FirebaseTables: Error asignando orden a mesa: ${e.message}")
            throw e
        }
    }

    override suspend fun clearTable(tableId: Int) {
        try {
            timber.log.Timber.d("🧹 FirebaseTables: Limpiando mesa $tableId")

            val updates = mapOf(
                "status" to TableStatus.LIBRE.name,
                "currentOrderId" to null,
                "updatedAt" to System.currentTimeMillis()
            )
            tablesRef.child(tableId.toString()).updateChildren(updates).await()

            timber.log.Timber.d("✅ FirebaseTables: Mesa limpiada exitosamente")
        } catch (e: Exception) {
            timber.log.Timber.d("❌ FirebaseTables: Error limpiando mesa: ${e.message}")
            throw e
        }
    }

    override suspend fun getTableById(tableId: Int): Table? {
        return try {
            timber.log.Timber.d("🔍 FirebaseTables: Buscando mesa por ID: $tableId")
            val snapshot = tablesRef.child(tableId.toString()).get().await()
            val table = snapshot.toTable()
            if (table != null) {
                timber.log.Timber.d("✅ FirebaseTables: Mesa encontrada: ${table.number}")
            } else {
                timber.log.Timber.d("❌ FirebaseTables: Mesa no encontrada: $tableId")
            }
            table
        } catch (e: Exception) {
            timber.log.Timber.d("❌ FirebaseTables: Error buscando mesa $tableId: ${e.message}")
            null
        }
    }

    override suspend fun updateTable(table: Table) {
        try {
            timber.log.Timber.d("🔄 FirebaseTables: Actualizando mesa: ${table.number}")

            val tableMap = table.toFirebaseMap()
            tablesRef.child(table.id.toString()).updateChildren(tableMap).await()

            timber.log.Timber.d("✅ FirebaseTables: Mesa actualizada exitosamente")
        } catch (e: Exception) {
            timber.log.Timber.d("❌ FirebaseTables: Error actualizando mesa: ${e.message}")
            throw e
        }
    }

    // ==================== MÉTODOS ADICIONALES REQUERIDOS ====================

    override suspend fun syncPendingTables() {
        try {
            timber.log.Timber.d("🔄 FirebaseTables: Sincronizando mesas pendientes...")
            val snapshot = tablesRef.orderByChild("syncStatus").equalTo("PENDING").get().await()
            val pendingTables = snapshot.children.mapNotNull { it.toTable() }

            if (pendingTables.isNotEmpty()) {
                timber.log.Timber.d("📤 FirebaseTables: ${pendingTables.size} mesas pendientes encontradas")
                pendingTables.forEach { table ->
                    timber.log.Timber.d("   - Mesa ${table.number}: ${table.status}")
                }
            } else {
                timber.log.Timber.d("✅ FirebaseTables: No hay mesas pendientes")
            }
        } catch (e: Exception) {
            timber.log.Timber.d("❌ FirebaseTables: Error en syncPendingTables: ${e.message}")
            throw e
        }
    }

    // ==================== MÉTODOS DE INICIALIZACIÓN ====================

    override suspend fun initializeDefaultTables() {
        try {
            timber.log.Timber.d("🔄 FirebaseTables: Inicializando mesas por defecto...")

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

                timber.log.Timber.d("🆕 FirebaseTables: Creando ${defaultTables.size} mesas por defecto")

                defaultTables.forEach { table ->
                    tablesRef.child(table.id.toString()).setValue(table.toFirebaseMap()).await()
                }

                timber.log.Timber.d("✅ FirebaseTables: Mesas inicializadas exitosamente")
            } else {
                timber.log.Timber.d("ℹ️ FirebaseTables: Las mesas ya existen, omitiendo inicialización")
            }

        } catch (e: Exception) {
            timber.log.Timber.d("❌ FirebaseTables: Error inicializando mesas: ${e.message}")
            throw e
        }
    }

    override suspend fun getTablesCount(): Int {
        return try {
            val snapshot = tablesRef.get().await()
            val count = snapshot.childrenCount.toInt()
            timber.log.Timber.d("📊 FirebaseTables: Total de mesas: $count")
            count
        } catch (e: Exception) {
            timber.log.Timber.d("❌ FirebaseTables: Error obteniendo conteo de mesas: ${e.message}")
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

            timber.log.Timber.d("🐛 FirebaseTables: Debug info generada")
            debugInfo
        } catch (e: Exception) {
            val errorMsg = "Error: ${e.message}"
            timber.log.Timber.d("❌ FirebaseTables: $errorMsg")
            errorMsg
        }
    }

    // ==================== MÉTODOS ESPECÍFICOS DE FIREBASE ====================

    override fun listenToTableChanges(): Flow<Table> = callbackFlow {
        val eventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { child ->
                    child.toTable()?.let { table ->
                        timber.log.Timber.d("📡 FirebaseTables: Cambio detectado en mesa ${table.number}")
                        trySend(table)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                timber.log.Timber.d("❌ FirebaseTables: Error en listenToTableChanges: ${error.message}")
                close(error.toException())
            }
        }

        tablesRef.addValueEventListener(eventListener)
        awaitClose { tablesRef.removeEventListener(eventListener) }
    }

    override fun getTablesRealTime(): Flow<List<Table>> = getTables()

    // ==================== MÉTODOS UTILITARIOS ====================

    private fun DataSnapshot.toTable(): Table? {
        return try {
            val idStr = key ?: return null
            val id = idStr.toIntOrNull() ?: return null
            val number = child("number").getValue(Int::class.java) ?: 0
            val statusStr = child("status").getValue(String::class.java) ?: "LIBRE"
            val status = try {
                TableStatus.valueOf(statusStr)
            } catch (e: IllegalArgumentException) {
                TableStatus.LIBRE
            }
            val currentOrderId = child("currentOrderId").getValue(String::class.java)
            val capacity = child("capacity").getValue(Int::class.java) ?: 4
            val version = child("version").getValue(Long::class.java) ?: 0
            val syncStatus = child("syncStatus").getValue(String::class.java) ?: "SYNCED"
            val updatedAt = child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

            Table(
                id = id,
                number = number,
                status = status,
                currentOrderId = currentOrderId,
                capacity = capacity,
                version = version,
                syncStatus = syncStatus
            )
        } catch (e: Exception) {
            timber.log.Timber.d("❌ FirebaseTables: Error convirtiendo DataSnapshot: ${e.message}")
            null
        }
    }

    private fun Table.toFirebaseMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "number" to number,
            "status" to status.name,
            "currentOrderId" to currentOrderId,
            "capacity" to capacity,
            "version" to version,
            "syncStatus" to syncStatus,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    // ==================== MÉTODOS ADICIONALES ÚTILES ====================

    suspend fun getTablesByStatus(status: TableStatus): List<Table> {
        return try {
            timber.log.Timber.d("🔍 FirebaseTables: Buscando mesas con estado: $status")
            val snapshot = tablesRef.orderByChild("status").equalTo(status.name).get().await()
            val tables = snapshot.children.mapNotNull { it.toTable() }
            timber.log.Timber.d("✅ FirebaseTables: ${tables.size} mesas encontradas con estado $status")
            tables
        } catch (e: Exception) {
            timber.log.Timber.d("❌ FirebaseTables: Error obteniendo mesas por estado: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAvailableTables(): List<Table> {
        return getTablesByStatus(TableStatus.LIBRE)
    }

    suspend fun getOccupiedTables(): List<Table> {
        return getTablesByStatus(TableStatus.OCUPADA)
    }

    suspend fun reserveTable(tableId: Int) {
        try {
            timber.log.Timber.d("📅 FirebaseTables: Reservando mesa $tableId")

            val updates = mapOf(
                "status" to TableStatus.RESERVADA.name,
                "updatedAt" to System.currentTimeMillis()
            )
            tablesRef.child(tableId.toString()).updateChildren(updates).await()

            timber.log.Timber.d("✅ FirebaseTables: Mesa reservada exitosamente")
        } catch (e: Exception) {
            timber.log.Timber.d("❌ FirebaseTables: Error reservando mesa: ${e.message}")
            throw e
        }
    }

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
            timber.log.Timber.d("❌ FirebaseTables: Error obteniendo estadísticas: ${e.message}")
            emptyMap()
        }
    }

    suspend fun tableExists(tableId: Int): Boolean {
        return try {
            val snapshot = tablesRef.child(tableId.toString()).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            timber.log.Timber.d("❌ FirebaseTables: Error verificando existencia de mesa: ${e.message}")
            false
        }
    }

    suspend fun getTableByNumber(tableNumber: Int): Table? {
        return try {
            timber.log.Timber.d("🔍 FirebaseTables: Buscando mesa por número: $tableNumber")
            val snapshot = tablesRef.orderByChild("number").equalTo(tableNumber.toDouble()).get().await()
            val table = snapshot.children.firstOrNull()?.toTable()
            if (table != null) {
                timber.log.Timber.d("✅ FirebaseTables: Mesa encontrada: ${table.number}")
            } else {
                timber.log.Timber.d("❌ FirebaseTables: Mesa no encontrada con número: $tableNumber")
            }
            table
        } catch (e: Exception) {
            timber.log.Timber.d("❌ FirebaseTables: Error buscando mesa por número: ${e.message}")
            null
        }
    }
}

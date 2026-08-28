package com.laprevia.restobar.data.repository

import com.laprevia.restobar.data.model.Table
import com.laprevia.restobar.data.model.TableStatus
import com.laprevia.restobar.domain.repository.FirebaseTableRepository
import com.laprevia.restobar.platform.currentTimeMillis
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.database.DataSnapshot
import dev.gitlive.firebase.database.DatabaseReference
import dev.gitlive.firebase.database.database
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Repositorio de mesas MULTIPLATAFORMA (GitLive Firebase): funciona en Android,
 * Desktop/JVM y Web JS. Reemplaza a la implementacion del SDK nativo de Android.
 *
 * IMPORTANTE: conserva EXACTAMENTE el mismo formato de datos que la implementacion
 * nativa (mismos nombres de campo y tipos), porque la base en produccion ya tiene
 * datos y otros dispositivos pueden seguir usando versiones anteriores.
 */
class GitLiveTableRepository : FirebaseTableRepository {

    private val tablesRef: DatabaseReference get() = Firebase.database.reference("tables")

    // ==================== LISTADO / TIEMPO REAL ====================

    override fun getTables(): Flow<List<Table>> =
        tablesRef.valueEvents.map { snapshot -> snapshot.children.mapNotNull { it.toTable() } }

    override fun getTablesRealTime(): Flow<List<Table>> = getTables()

    override fun listenToTableChanges(): Flow<Table> =
        tablesRef.valueEvents.map { snapshot ->
            snapshot.children.mapNotNull { it.toTable() }
        }.map { it.lastOrNull() ?: Table() }

    override fun getPendingTables(): Flow<List<Table>> =
        tablesRef.orderByChild("syncStatus").equalTo("PENDING").valueEvents
            .map { snapshot -> snapshot.children.mapNotNull { it.toTable() } }

    // ==================== BUSQUEDA ====================

    override suspend fun getTableById(tableId: Int): Table? =
        runCatching { tablesRef.child(tableId.toString()).valueEvents.first().toTable() }.getOrNull()

    override suspend fun getTablesCount(): Int =
        runCatching { tablesRef.valueEvents.first().children.count() }.getOrDefault(0)

    // ==================== ESTADOS ====================

    override suspend fun updateTableStatus(tableId: Int, status: TableStatus) {
        tablesRef.child(tableId.toString()).updateChildren(
            mapOf(
                "status" to status.name,
                "updatedAt" to currentTimeMillis()
            )
        )
    }

    override suspend fun assignOrderToTable(tableId: Int, orderId: String) {
        tablesRef.child(tableId.toString()).updateChildren(
            mapOf(
                "status" to TableStatus.OCUPADA.name,
                "currentOrderId" to orderId,
                "updatedAt" to currentTimeMillis()
            )
        )
    }

    override suspend fun clearTable(tableId: Int) {
        tablesRef.child(tableId.toString()).updateChildren(
            mapOf(
                "status" to TableStatus.LIBRE.name,
                "currentOrderId" to null,
                "updatedAt" to currentTimeMillis()
            )
        )
    }

    override suspend fun updateTable(table: Table) {
        tablesRef.child(table.id.toString()).updateChildren(table.toFirebaseMap())
    }

    // ==================== INICIALIZACION ====================

    override suspend fun initializeDefaultTables() {
        val snapshot = tablesRef.valueEvents.first()
        if (!snapshot.exists || snapshot.children.count() == 0) {
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
            defaultTables.forEach { table ->
                tablesRef.child(table.id.toString()).updateChildren(table.toFirebaseMap())
            }
        }
    }

    // ==================== SYNC / DEBUG ====================

    override suspend fun syncPendingTables() {
        // Igual que la implementacion nativa: solo inspecciona pendientes (log-only).
        runCatching {
            tablesRef.orderByChild("syncStatus").equalTo("PENDING").valueEvents.first()
        }
    }

    override suspend fun debugTables(): String = runCatching {
        val tables = tablesRef.valueEvents.first().children.mapNotNull { it.toTable() }
        buildString {
            append("Mesas en Firebase: ${tables.size}\n")
            tables.forEach { table ->
                append("Mesa ${table.number}: ${table.status}")
                table.currentOrderId?.let { append(" (Orden: ${it.take(8)}...)") }
                appendLine()
            }
        }
    }.getOrElse { "Error: ${it.message}" }

    // ==================== CONVERSION (mismo wire format que el SDK nativo) ====================

    private fun DataSnapshot.toTable(): Table? = runCatching {
        val id = key?.toIntOrNull() ?: return null
        // Lectura cruda con conversion tolerante: Firebase entrega enteros como Long.
        val status = runCatching {
            TableStatus.valueOf(child("status").value as? String ?: "LIBRE")
        }.getOrDefault(TableStatus.LIBRE)
        Table(
            id = id,
            number = (child("number").value as? Number)?.toInt() ?: 0,
            status = status,
            currentOrderId = child("currentOrderId").value as? String,
            capacity = (child("capacity").value as? Number)?.toInt() ?: 4,
            version = (child("version").value as? Number)?.toLong() ?: 0,
            syncStatus = child("syncStatus").value as? String ?: "SYNCED"
        )
    }.getOrNull()

    private fun Table.toFirebaseMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "number" to number,
        "status" to status.name,
        "currentOrderId" to currentOrderId,
        "capacity" to capacity,
        "version" to version,
        "syncStatus" to syncStatus,
        "updatedAt" to currentTimeMillis()
    )
}

package com.laprevia.restobar.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.laprevia.restobar.data.local.dao.AppErrorLogDao
import com.laprevia.restobar.data.local.dao.AuditLogDao
import com.laprevia.restobar.data.local.dao.CashClosureDao
import com.laprevia.restobar.data.local.dao.InventoryDao
import com.laprevia.restobar.data.local.dao.OrderDao
import com.laprevia.restobar.data.local.dao.ProductDao
import com.laprevia.restobar.data.local.dao.TableDao
import com.laprevia.restobar.data.local.entity.AppErrorLogEntity
import com.laprevia.restobar.data.local.entity.AuditLogEntity
import com.laprevia.restobar.data.local.entity.CashClosureEntity
import com.laprevia.restobar.data.local.entity.InventoryEntity
import com.laprevia.restobar.data.local.entity.OrderEntity
import com.laprevia.restobar.data.local.entity.ProductEntity
import com.laprevia.restobar.data.local.entity.TableEntity

/**
 * Base de datos local MULTIPLATAFORMA (Room KMP): Android + Desktop/JVM.
 * Las migraciones usan la API de driver (SQLiteConnection) que funciona en
 * todos los targets; el SQL es identico al de las migraciones originales.
 */
@Database(
    entities = [
        OrderEntity::class,
        ProductEntity::class,
        TableEntity::class,
        InventoryEntity::class,
        AuditLogEntity::class,
        CashClosureEntity::class,
        AppErrorLogEntity::class
    ],
    version = 12
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun orderDao(): OrderDao
    abstract fun productDao(): ProductDao
    abstract fun tableDao(): TableDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun cashClosureDao(): CashClosureDao
    abstract fun appErrorLogDao(): AppErrorLogDao

    companion object {

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE orders ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'UNSPECIFIED'")
                connection.execSQL("ALTER TABLE orders ADD COLUMN paidAt INTEGER")
                connection.execSQL("ALTER TABLE orders ADD COLUMN receiptNumber TEXT")
                connection.execSQL("CREATE TABLE IF NOT EXISTS audit_logs (id TEXT NOT NULL PRIMARY KEY, action TEXT NOT NULL, actorRole TEXT NOT NULL, actorName TEXT NOT NULL, targetType TEXT NOT NULL, targetId TEXT NOT NULL, detail TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                connection.execSQL("CREATE TABLE IF NOT EXISTS cash_closures (id TEXT NOT NULL PRIMARY KEY, periodStart INTEGER NOT NULL, periodEnd INTEGER NOT NULL, totalSales REAL NOT NULL, grossProfit REAL NOT NULL, chargedOrders INTEGER NOT NULL, cancelledOrders INTEGER NOT NULL, productsSold INTEGER NOT NULL, createdBy TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                connection.execSQL("CREATE TABLE IF NOT EXISTS app_error_logs (id TEXT NOT NULL PRIMARY KEY, source TEXT NOT NULL, message TEXT NOT NULL, detail TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(connection: SQLiteConnection) {
                addColumnIfMissing(connection, "cash_closures", "cashSales", "REAL NOT NULL DEFAULT 0")
                addColumnIfMissing(connection, "cash_closures", "yapePlinSales", "REAL NOT NULL DEFAULT 0")
                addColumnIfMissing(connection, "cash_closures", "cardSales", "REAL NOT NULL DEFAULT 0")
                addColumnIfMissing(connection, "cash_closures", "bestSellingProduct", "TEXT NOT NULL DEFAULT 'Sin ventas'")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE orders ADD COLUMN amountReceived REAL")
                connection.execSQL("ALTER TABLE orders ADD COLUMN changeGiven REAL")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE orders ADD COLUMN discountAmount REAL")
                connection.execSQL("ALTER TABLE orders ADD COLUMN discountReason TEXT")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(connection: SQLiteConnection) {
                addColumnIfMissing(connection, "cash_closures", "openingAmount", "REAL NOT NULL DEFAULT 0")
                addColumnIfMissing(connection, "cash_closures", "incomeAmount", "REAL NOT NULL DEFAULT 0")
                addColumnIfMissing(connection, "cash_closures", "expenseAmount", "REAL NOT NULL DEFAULT 0")
                addColumnIfMissing(connection, "cash_closures", "expectedCash", "REAL NOT NULL DEFAULT 0")
                addColumnIfMissing(connection, "cash_closures", "actualCash", "REAL NOT NULL DEFAULT 0")
                addColumnIfMissing(connection, "cash_closures", "cashDifference", "REAL NOT NULL DEFAULT 0")
            }
        }

        private fun addColumnIfMissing(
            connection: SQLiteConnection,
            tableName: String,
            columnName: String,
            columnDefinition: String
        ) {
            val statement = connection.prepare("PRAGMA table_info(`$tableName`)")
            val columnExists = try {
                var found = false
                while (statement.step()) {
                    if (statement.getText(1) == columnName) {
                        found = true
                        break
                    }
                }
                found
            } finally {
                statement.close()
            }

            if (!columnExists) {
                connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinition")
            }
        }

        // Lista de migraciones para reutilizar en los builders por plataforma.
        val MIGRATIONS: Array<Migration> =
            arrayOf(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)
    }
}

// El compilador de Room genera la implementacion `actual` por target.
@Suppress("KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

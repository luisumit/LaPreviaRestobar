package com.laprevia.restobar.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.laprevia.restobar.data.local.dao.*
import com.laprevia.restobar.data.local.entity.*

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
    version = 11
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun orderDao(): OrderDao
    abstract fun productDao(): ProductDao
    abstract fun tableDao(): TableDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun cashClosureDao(): CashClosureDao
    abstract fun appErrorLogDao(): AppErrorLogDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE orders ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'UNSPECIFIED'")
                db.execSQL("ALTER TABLE orders ADD COLUMN paidAt INTEGER")
                db.execSQL("ALTER TABLE orders ADD COLUMN receiptNumber TEXT")
                db.execSQL("CREATE TABLE IF NOT EXISTS audit_logs (id TEXT NOT NULL PRIMARY KEY, action TEXT NOT NULL, actorRole TEXT NOT NULL, actorName TEXT NOT NULL, targetType TEXT NOT NULL, targetId TEXT NOT NULL, detail TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS cash_closures (id TEXT NOT NULL PRIMARY KEY, periodStart INTEGER NOT NULL, periodEnd INTEGER NOT NULL, totalSales REAL NOT NULL, grossProfit REAL NOT NULL, chargedOrders INTEGER NOT NULL, cancelledOrders INTEGER NOT NULL, productsSold INTEGER NOT NULL, createdBy TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS app_error_logs (id TEXT NOT NULL PRIMARY KEY, source TEXT NOT NULL, message TEXT NOT NULL, detail TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cash_closures ADD COLUMN cashSales REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cash_closures ADD COLUMN yapePlinSales REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cash_closures ADD COLUMN cardSales REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cash_closures ADD COLUMN bestSellingProduct TEXT NOT NULL DEFAULT 'Sin ventas'")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE orders ADD COLUMN amountReceived REAL")
                db.execSQL("ALTER TABLE orders ADD COLUMN changeGiven REAL")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE orders ADD COLUMN discountAmount REAL")
                db.execSQL("ALTER TABLE orders ADD COLUMN discountReason TEXT")
            }
        }

        // Lista de migraciones para reutilizar (get() y el modulo de Hilt).
        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11)

        fun get(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "restobar_db"
                )
                    .addMigrations(*MIGRATIONS)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}

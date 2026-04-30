package com.laprevia.restobar.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.laprevia.restobar.data.local.dao.*
import com.laprevia.restobar.data.local.entity.*

@Database(
    entities = [
        OrderEntity::class,
        ProductEntity::class,
        TableEntity::class,
        InventoryEntity::class
    ],
    version = 7 // ✅ CAMBIO 1: Aumentar versión de 1 a 2
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun orderDao(): OrderDao
    abstract fun productDao(): ProductDao
    abstract fun tableDao(): TableDao
    abstract fun inventoryDao(): InventoryDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "restobar_db"
                ).fallbackToDestructiveMigration()  // ✅ CAMBIO 2: Agregar esta línea
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
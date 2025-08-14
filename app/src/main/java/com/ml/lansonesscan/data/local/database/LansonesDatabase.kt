package com.ml.lansonesscan.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.ml.lansonesscan.data.local.dao.ScanDao
import com.ml.lansonesscan.data.local.entities.ScanResultEntity

/**
 * Room database for the Lansones Scanner app
 */
@Database(
    entities = [ScanResultEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(com.ml.lansonesscan.data.local.database.Converters::class)
abstract class LansonesDatabase : RoomDatabase() {

    abstract fun scanDao(): ScanDao

    companion object {
        @Volatile
        private var INSTANCE: LansonesDatabase? = null

        private const val DATABASE_NAME = "lansones_database"

        /**
         * Migration from version 1 to 2 (example for future use)
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example migration - add new column
                // database.execSQL("ALTER TABLE scan_results ADD COLUMN new_column TEXT")
            }
        }

        fun getDatabase(context: Context): LansonesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LansonesDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration() // For development only
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * For testing purposes - creates an in-memory database
         */
        fun getInMemoryDatabase(context: Context): LansonesDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                LansonesDatabase::class.java
            ).allowMainThreadQueries().build()
        }

        /**
         * Clear the database instance (useful for testing)
         */
        fun clearInstance() {
            INSTANCE = null
        }
    }
}
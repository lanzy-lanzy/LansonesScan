package com.ml.lansonesscan.di

import android.content.Context
import com.ml.lansonesscan.data.local.dao.ScanDao
import com.ml.lansonesscan.data.local.database.LansonesDatabase

/**
 * Database module for dependency injection
 * This is a simple implementation without Hilt/Dagger for now
 */
object DatabaseModule {

    @Volatile
    private var database: LansonesDatabase? = null

    /**
     * Provides the Room database instance
     */
    fun provideDatabase(context: Context): LansonesDatabase {
        return database ?: synchronized(this) {
            val instance = LansonesDatabase.getDatabase(context)
            database = instance
            instance
        }
    }

    /**
     * Provides the ScanDao
     */
    fun provideScanDao(context: Context): ScanDao {
        return provideDatabase(context).scanDao()
    }

    /**
     * For testing - provides in-memory database
     */
    fun provideInMemoryDatabase(context: Context): LansonesDatabase {
        return LansonesDatabase.getInMemoryDatabase(context)
    }

    /**
     * Clear database instance (useful for testing)
     */
    fun clearDatabase() {
        database?.close()
        database = null
        LansonesDatabase.clearInstance()
    }
}

/**
 * Extension function to get database from context
 */
fun Context.getDatabase(): LansonesDatabase {
    return DatabaseModule.provideDatabase(this)
}

/**
 * Extension function to get ScanDao from context
 */
fun Context.getScanDao(): ScanDao {
    return DatabaseModule.provideScanDao(this)
}
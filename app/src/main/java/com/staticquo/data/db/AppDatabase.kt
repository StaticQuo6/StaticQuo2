package com.staticquo.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.staticquo.heatmap.HeatmapDao
import com.staticquo.heatmap.HeatmapEntity
import javax.inject.Inject
import javax.inject.Singleton

@Database(
    entities = [AppLockEntity::class, MapRegionEntity::class, VaultEntryEntity::class, HeatmapEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appLockDao(): AppLockDao
    abstract fun mapRegionDao(): MapRegionDao
    abstract fun vaultEntryDao(): VaultEntryDao
    abstract fun heatmapDao(): HeatmapDao

    companion object {
        const val DB_NAME = "staticquo.db"
    }
}

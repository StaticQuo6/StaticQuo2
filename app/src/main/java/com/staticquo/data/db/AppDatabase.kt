package com.staticquo.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import javax.inject.Inject
import javax.inject.Singleton

@Database(
    entities = [AppLockEntity::class, MapRegionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appLockDao(): AppLockDao
    abstract fun mapRegionDao(): MapRegionDao

    companion object {
        const val DB_NAME = "staticquo.db"
    }
}

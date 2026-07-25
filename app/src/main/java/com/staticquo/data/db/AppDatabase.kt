package com.staticquo.data.db

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.staticquo.heatmap.HeatmapDao
import com.staticquo.heatmap.HeatmapEntity
// TODO: Re-enable when routing is reintroduced
// import com.staticquo.routing.RoutingDao
// import com.staticquo.routing.RoutingRegionEntity
import com.staticquo.search.SearchDao
import com.staticquo.search.SearchDocument
import com.staticquo.search.SearchIndexFts
import javax.inject.Inject
import javax.inject.Singleton

@Database(
    entities = [
        AppLockEntity::class,
        MapRegionEntity::class,
        VaultEntryEntity::class,
        HeatmapEntity::class,
        SearchDocument::class,
        SearchIndexFts::class,
        // RoutingRegionEntity::class  // TODO: re-enable with routing
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appLockDao(): AppLockDao
    abstract fun mapRegionDao(): MapRegionDao
    abstract fun vaultEntryDao(): VaultEntryDao
    abstract fun heatmapDao(): HeatmapDao
    // abstract fun routingDao(): RoutingDao  // TODO: re-enable with routing
    abstract fun searchDao(): SearchDao

    companion object {
        const val DB_NAME = "staticquo.db"
    }
}

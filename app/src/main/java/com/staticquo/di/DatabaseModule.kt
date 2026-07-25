package com.staticquo.di

import android.content.Context
import androidx.room.Room
import com.staticquo.data.db.AppDatabase
import com.staticquo.data.db.AppLockDao
import com.staticquo.data.db.MapRegionDao
import com.staticquo.data.db.VaultEntryDao
import com.staticquo.heatmap.HeatmapDao
// TODO: Re-enable when routing is reintroduced
// import com.staticquo.routing.RoutingDao
import com.staticquo.search.SearchDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room
            .databaseBuilder(context, AppDatabase::class.java, AppDatabase.DB_NAME)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideAppLockDao(db: AppDatabase): AppLockDao = db.appLockDao()

    @Provides
    fun provideMapRegionDao(db: AppDatabase): MapRegionDao = db.mapRegionDao()

    @Provides
    fun provideVaultEntryDao(db: AppDatabase): VaultEntryDao = db.vaultEntryDao()

    @Provides
    fun provideHeatmapDao(db: AppDatabase): HeatmapDao = db.heatmapDao()

    // @Provides  // TODO: re-enable with routing
    // fun provideRoutingDao(db: AppDatabase): RoutingDao = db.routingDao()

    @Provides
    fun provideSearchDao(db: AppDatabase): SearchDao = db.searchDao()
}

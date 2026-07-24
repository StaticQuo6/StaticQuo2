package com.staticquo.di

import android.content.Context
import androidx.room.Room
import com.staticquo.data.db.AppDatabase
import com.staticquo.data.db.AppLockDao
import com.staticquo.data.db.MapRegionDao
import com.staticquo.data.db.VaultEntryDao
import com.staticquo.heatmap.HeatmapDao
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
}

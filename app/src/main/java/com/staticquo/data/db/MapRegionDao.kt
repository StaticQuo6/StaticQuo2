package com.staticquo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MapRegionDao {

    @Query("SELECT * FROM map_regions ORDER BY downloaded_at DESC")
    suspend fun getAll(): List<MapRegionEntity>

    @Query("SELECT * FROM map_regions WHERE is_downloaded = 1 ORDER BY downloaded_at DESC")
    suspend fun getDownloaded(): List<MapRegionEntity>

    @Query("SELECT * FROM map_regions WHERE id = :id")
    suspend fun getById(id: String): MapRegionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(region: MapRegionEntity)

    @Query("UPDATE map_regions SET is_downloaded = 1, mbtiles_path = :path, downloaded_at = :now WHERE id = :id")
    suspend fun markDownloaded(id: String, path: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM map_regions WHERE id = :id")
    suspend fun delete(id: String)
}

package com.staticquo.heatmap

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HeatmapDao {

    @Query("SELECT * FROM heatmap_beacons ORDER BY created_at DESC")
    suspend fun getAll(): List<HeatmapEntity>

    @Query("SELECT * FROM heatmap_beacons WHERE beacon_type = :type ORDER BY created_at DESC")
    suspend fun getByType(type: String): List<HeatmapEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(beacon: HeatmapEntity): Long

    @Query("DELETE FROM heatmap_beacons WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM heatmap_beacons")
    suspend fun clearAll()
}

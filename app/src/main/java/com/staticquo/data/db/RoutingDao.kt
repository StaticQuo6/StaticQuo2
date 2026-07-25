package com.staticquo.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RoutingDao {

    @Query("SELECT * FROM routing_regions WHERE id = :id")
    suspend fun getById(id: String): RoutingRegionEntity?

    @Query("SELECT * FROM routing_regions ORDER BY downloaded_at DESC")
    suspend fun getAll(): List<RoutingRegionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RoutingRegionEntity)

    @Query("DELETE FROM routing_regions WHERE id = :id")
    suspend fun delete(id: String)
}

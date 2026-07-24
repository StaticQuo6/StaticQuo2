package com.staticquo.routing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RoutingDao {

    @Query("SELECT * FROM routing_regions ORDER BY downloaded_at DESC LIMIT 1")
    suspend fun getActiveRegion(): RoutingRegionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegion(region: RoutingRegionEntity)

    @Query("DELETE FROM routing_regions")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM routing_regions")
    suspend fun count(): Int
}

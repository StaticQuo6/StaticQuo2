package com.staticquo.routing

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routing_regions")
data class RoutingRegionEntity(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "tiles_path")
    val tilesPath: String,

    @ColumnInfo(name = "config_path")
    val configPath: String,

    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long = System.currentTimeMillis()
)

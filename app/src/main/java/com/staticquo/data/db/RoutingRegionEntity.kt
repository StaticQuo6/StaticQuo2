package com.staticquo.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routing_regions")
data class RoutingRegionEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "graph_path")
    val graphPath: String,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,

    @ColumnInfo(name = "version")
    val version: String,

    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long = System.currentTimeMillis()
)

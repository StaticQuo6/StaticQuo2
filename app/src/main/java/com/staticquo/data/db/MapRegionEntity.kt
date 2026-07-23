package com.staticquo.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "map_regions")
data class MapRegionEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "mbtiles_path")
    val mbtilesPath: String,

    @ColumnInfo(name = "size_bytes")
    val sizeBytes: Long,

    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "version")
    val version: String,

    @ColumnInfo(name = "is_downloaded")
    val isDownloaded: Boolean = false
)

package com.staticquo.settings

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class TileCoord(val z: Int, val x: Int, val y: Int)

object TileMath {
    fun latLonToTile(lat: Double, lon: Double, zoom: Int): TileCoord {
        val latRad = Math.toRadians(lat)
        val n = 1 shl zoom
        val x = ((lon + 180.0) / 360.0 * n).toInt()
        val y = ((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n).toInt()
        return TileCoord(zoom, x.coerceIn(0, n - 1), y.coerceIn(0, n - 1))
    }

    fun tileBounds(south: Double, north: Double, west: Double, east: Double, zoom: Int): Pair<TileCoord, TileCoord> {
        val min = latLonToTile(north, west, zoom)
        val max = latLonToTile(south, east, zoom)
        return TileCoord(zoom, min.x.coerceAtMost(max.x), min.y.coerceAtMost(max.y)) to
                TileCoord(zoom, max.x.coerceAtLeast(min.x), max.y.coerceAtLeast(min.y))
    }

    fun estimateTileCount(south: Double, north: Double, west: Double, east: Double, minZoom: Int, maxZoom: Int): Long {
        var count = 0L
        for (z in minZoom..maxZoom) {
            val (minT, maxT) = tileBounds(south, north, west, east, z)
            count += (maxT.x - minT.x + 1).toLong() * (maxT.y - minT.y + 1).toLong()
        }
        return count
    }
}

class MbtilesBuilder(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TILE_URL = "https://tile.openstreetmap.org/%d/%d/%d.png"
        private const val USER_AGENT = "StaticQuo/1.0"
    }

    data class BuildProgress(
        val currentTile: Int,
        val totalTiles: Int,
        val percentage: Float,
        val message: String
    )

    fun build(
        outputFile: File,
        south: Double, north: Double, west: Double, east: Double,
        minZoom: Int, maxZoom: Int,
        regionName: String,
        onProgress: (BuildProgress) -> Unit
    ): Result<File> {
        return try {
            outputFile.parentFile?.mkdirs()
            outputFile.delete()

            val db = SQLiteDatabase.openOrCreateDatabase(outputFile, null)
            db.execSQL("CREATE TABLE metadata (name TEXT, value TEXT)")
            db.execSQL("CREATE TABLE tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB)")
            db.execSQL("CREATE UNIQUE INDEX tile_index ON tiles (zoom_level, tile_column, tile_row)")

            val metaValues = ContentValues().apply {
                put("name", "name"); put("value", regionName)
            }
            db.insert("metadata", null, metaValues)
            db.insert("metadata", null, ContentValues().apply {
                put("name", "type"); put("value", "baselayer")
            })
            db.insert("metadata", null, ContentValues().apply {
                put("name", "version"); put("value", "1.0.0")
            })
            db.insert("metadata", null, ContentValues().apply {
                put("name", "format"); put("value", "png")
            })
            db.insert("metadata", null, ContentValues().apply {
                put("name", "bounds"); put("value", "$west,$south,$east,$north")
            })
            val centerLon = (west + east) / 2.0
            val centerLat = (south + north) / 2.0
            db.insert("metadata", null, ContentValues().apply {
                put("name", "center"); put("value", "$centerLon,$centerLat,$minZoom")
            })

            val tiles = mutableListOf<TileCoord>()
            for (z in minZoom..maxZoom) {
                val (minT, maxT) = TileMath.tileBounds(south, north, west, east, z)
                for (x in minT.x..maxT.x) {
                    for (y in minT.y..maxT.y) {
                        tiles.add(TileCoord(z, x, y))
                    }
                }
            }

            val total = tiles.size
            var downloaded = 0
            var errors = 0

            for (tile in tiles) {
                val url = TILE_URL.format(tile.z, tile.x, tile.y)
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .build()

                try {
                    val response = httpClient.newCall(request).execute()
                    if (response.isSuccessful) {
                        response.body?.bytes()?.let { data ->
                            val rowValues = ContentValues().apply {
                                put("zoom_level", tile.z)
                                put("tile_column", tile.x)
                                put("tile_row", (1 shl tile.z) - 1 - tile.y)
                                put("tile_data", data)
                            }
                            db.insert("tiles", null, rowValues)
                        }
                    }
                    response.close()
                } catch (_: Exception) {
                    errors++
                }

                downloaded++
                onProgress(BuildProgress(
                    currentTile = downloaded,
                    totalTiles = total,
                    percentage = downloaded.toFloat() / total,
                    message = "Downloading tiles: $downloaded / $total"
                ))
            }

            db.execSQL("ANALYZE")
            db.close()

            if (downloaded - errors == 0) {
                outputFile.delete()
                return Result.failure(Exception("No tiles could be downloaded"))
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            if (outputFile.exists()) outputFile.delete()
            Result.failure(e)
        }
    }
}

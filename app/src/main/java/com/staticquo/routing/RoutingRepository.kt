package com.staticquo.routing

import android.content.Context
import com.valhalla.valhalla.ValhallaActor
import com.valhalla.valhalla.files.ValhallaFile
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class RoutePoint(
    val latitude: Double,
    val longitude: Double
)

data class RouteInfo(
    val points: List<RoutePoint>,
    val distanceKm: Double,
    val durationSeconds: Double
)

sealed class RoutingResult<T> {
    data class Success<T>(val data: T) : RoutingResult<T>()
    data class Error<T>(val message: String) : RoutingResult<T>()
}

@Singleton
class RoutingRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var actor: ValhallaActor? = null
    private val activeRegionFile: File
        get() = File(context.filesDir, "routing_tiles.tar")

    suspend fun hasTiles(): Boolean = activeRegionFile.exists()

    suspend fun installDemoIfNeeded() {
        if (activeRegionFile.exists()) return
        try {
            context.assets.open("routing/valhalla_tiles.tar").use { input ->
                activeRegionFile.parentFile?.mkdirs()
                activeRegionFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (_: Exception) {}
    }

    suspend fun isReady(): Boolean {
        return actor != null || activeRegionFile.exists()
    }

    fun initActor() {
        if (actor != null) return
        if (!activeRegionFile.exists()) return

        val configPath = writeConfig()
        actor = ValhallaActor(configPath)
    }

    fun route(
        origin: RoutePoint,
        destination: RoutePoint,
        costing: String = "pedestrian"
    ): RoutingResult<RouteInfo> {
        try {
            initActor()
            val act = actor ?: return RoutingResult.Error("No routing tiles available")

            val request = JSONObject().apply {
                put("locations", JSONArray().apply {
                    put(JSONObject().apply {
                        put("lat", origin.latitude)
                        put("lon", origin.longitude)
                    })
                    put(JSONObject().apply {
                        put("lat", destination.latitude)
                        put("lon", destination.longitude)
                    })
                })
                put("costing", costing)
                put("units", "kilometers")
            }

            val responseJson = act.route(request.toString())
            val response = JSONObject(responseJson)

            if (response.has("error") || (response.has("trip") && response.getJSONObject("trip").optInt("status", -1) != 0)) {
                val msg = response.optString("error", response.optJSONObject("trip")?.optString("status_message", "Route failed") ?: "Route failed")
                return RoutingResult.Error(msg)
            }

            val trip = response.getJSONObject("trip")
            val legs = trip.getJSONArray("legs")
            if (legs.length() == 0) return RoutingResult.Error("No route found")

            val leg = legs.getJSONObject(0)
            val shape = leg.optString("shape", "")
            val points = decodePolyline(shape)

            val summary = leg.getJSONObject("summary")
            val distanceKm = summary.optDouble("length", 0.0)
            val durationSeconds = summary.optDouble("time", 0.0)

            return RoutingResult.Success(
                RouteInfo(
                    points = points,
                    distanceKm = distanceKm,
                    durationSeconds = durationSeconds
                )
            )
        } catch (e: Exception) {
            return RoutingResult.Error("Routing error: ${e.message}")
        }
    }

    private fun writeConfig(): String {
        val configFile = ValhallaFile(context, "valhalla.json")
        val configJson = JSONObject().apply {
            put("mjolnir", JSONObject().apply {
                put("tile_dir", activeRegionFile.absolutePath)
                put("tile_extract", activeRegionFile.absolutePath)
            })
            put("loki", JSONObject().apply {
                put("actions", JSONArray(listOf("route")))
                put("logging", JSONObject().apply {
                    put("type", "std_out")
                    put("level", "none")
                })
            })
            put("thor", JSONObject().apply {
                put("logging", JSONObject().apply {
                    put("type", "std_out")
                    put("level", "none")
                })
            })
            put("odin", JSONObject().apply {
                put("logging", JSONObject().apply {
                    put("type", "std_out")
                    put("level", "none")
                })
            })
        }
        configFile.writeText(configJson.toString(2))
        return configFile.absolutePath()
    }

    fun downloadTiles(url: String, onProgress: (Float) -> Unit = {}): RoutingResult<Unit> {
        return try {
            val request = Request.Builder().url(url).get().build()
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return RoutingResult.Error("Download failed: HTTP ${response.code}")
            }
            val body = response.body ?: return RoutingResult.Error("Empty response")
            val total = body.contentLength()
            val buffer = body.byteStream().use { input ->
                val baos = ByteArrayOutputStream()
                val buf = ByteArray(8192)
                var read: Int
                var soFar = 0L
                while (input.read(buf).also { read = it } != -1) {
                    baos.write(buf, 0, read)
                    soFar += read
                    if (total > 0) {
                        onProgress(soFar.toFloat() / total)
                    }
                }
                baos.toByteArray()
            }
            activeRegionFile.parentFile?.mkdirs()
            activeRegionFile.writeBytes(buffer)
            actor = null
            RoutingResult.Success(Unit)
        } catch (e: Exception) {
            RoutingResult.Error("Download failed: ${e.message}")
        }
    }

    private fun decodePolyline(encoded: String): List<RoutePoint> {
        val points = mutableListOf<RoutePoint>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            points.add(RoutePoint(lat / 1e6, lng / 1e6))
        }
        return points
    }
}

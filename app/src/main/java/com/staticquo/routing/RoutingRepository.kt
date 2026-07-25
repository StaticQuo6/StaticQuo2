// TODO: Reintroduce offline routing. This file was stubbed because
// valhalla-mobile:0.5.1 was compiled with Kotlin 2.3.0 and is incompatible
// with our Kotlin 2.0.21 project. Evaluate either an older valhalla-mobile
// version compiled against Kotlin 2.0.x, or a different routing library.
// See https://github.com/StaticQuo6/StaticQuo2/issues/???

package com.staticquo.routing

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
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
    private val activeRegionFile: File
        get() = File(context.filesDir, "routing_tiles.tar")

    suspend fun hasTiles(): Boolean = false

    suspend fun installDemoIfNeeded() {}

    suspend fun isReady(): Boolean = false

    fun route(
        origin: RoutePoint,
        destination: RoutePoint,
        costing: String = "pedestrian"
    ): RoutingResult<RouteInfo> {
        return RoutingResult.Error("Routing not available — valhalla-mobile dependency removed")
    }

    fun downloadTiles(url: String, onProgress: (Float) -> Unit = {}): RoutingResult<Unit> {
        return RoutingResult.Error("Routing tile download not available — valhalla-mobile dependency removed")
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

package com.staticquo.routing

import com.graphhopper.util.shapes.GHPoint
import com.graphhopper.GHRequest
import com.graphhopper.GraphHopper
import com.graphhopper.ResponsePath
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class RouteResult(
    val points: List<Pair<Double, Double>>,
    val distanceMeters: Double,
    val timeMillis: Long,
    val path: ResponsePath
)

@Singleton
class GraphHopperEngine @Inject constructor() {

    private var hopper: GraphHopper? = null
    private var loadedPath: String? = null

    fun isLoaded(): Boolean = hopper != null

    fun getLoadedPath(): String? = loadedPath

    fun load(graphDirectory: String): Result<Unit> {
        return try {
            if (hopper != null) {
                hopper?.close()
                hopper = null
            }
            val dir = File(graphDirectory)
            if (!dir.exists() || !dir.isDirectory) {
                return Result.failure(Exception("Graph directory does not exist: $graphDirectory"))
            }
            val gh = GraphHopper()
            gh.setGraphHopperLocation(graphDirectory)
            gh.load()
            hopper = gh
            loadedPath = graphDirectory
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun route(
        fromLat: Double, fromLon: Double,
        toLat: Double, toLon: Double,
        profile: String = "car"
    ): Result<RouteResult> {
        val gh = hopper
        if (gh == null) return Result.failure(Exception("Graph not loaded"))

        return try {
            val request = GHRequest(
                GHPoint(fromLat, fromLon), GHPoint(toLat, toLon)
            ).setProfile(profile)

            val response = gh.route(request)

            if (response.hasErrors()) {
                val errMsg = response.errors.joinToString("; ") { it.message ?: it.toString() }
                return Result.failure(Exception(errMsg))
            }

            val best: ResponsePath = response.best ?: return Result.failure(Exception("No route found"))
            val points = best.points.map { Pair(it.lat, it.lon) }

            Result.success(
                RouteResult(
                    points = points,
                    distanceMeters = best.distance,
                    timeMillis = best.time,
                    path = best
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        hopper?.close()
        hopper = null
        loadedPath = null
    }
}

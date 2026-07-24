package com.staticquo.heatmap

import javax.inject.Inject
import javax.inject.Singleton

sealed class HeatmapResult<T> {
    data class Success<T>(val data: T) : HeatmapResult<T>()
    data class Error<T>(val message: String) : HeatmapResult<T>()
}

@Singleton
class HeatmapRepository @Inject constructor(
    private val dao: HeatmapDao
) {

    suspend fun getAllBeacons(): HeatmapResult<List<HeatmapEntity>> {
        return try {
            HeatmapResult.Success(dao.getAll())
        } catch (e: Exception) {
            HeatmapResult.Error("Failed to load beacons: ${e.message}")
        }
    }

    suspend fun addBeacon(
        latitude: Double,
        longitude: Double,
        type: BeaconType,
        title: String,
        description: String = "",
        sourceDeviceId: String = ""
    ): HeatmapResult<Long> {
        return try {
            val entity = HeatmapEntity(
                latitude = latitude,
                longitude = longitude,
                beaconType = type.name,
                title = title,
                description = description,
                sourceDeviceId = sourceDeviceId
            )
            val id = dao.insert(entity)
            HeatmapResult.Success(id)
        } catch (e: Exception) {
            HeatmapResult.Error("Failed to add beacon: ${e.message}")
        }
    }

    suspend fun deleteBeacon(id: Long): HeatmapResult<Unit> {
        return try {
            dao.delete(id)
            HeatmapResult.Success(Unit)
        } catch (e: Exception) {
            HeatmapResult.Error("Failed to delete beacon: ${e.message}")
        }
    }

    suspend fun clearAll(): HeatmapResult<Unit> {
        return try {
            dao.clearAll()
            HeatmapResult.Success(Unit)
        } catch (e: Exception) {
            HeatmapResult.Error("Failed to clear beacons: ${e.message}")
        }
    }
}

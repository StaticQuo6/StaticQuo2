package com.staticquo.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticquo.data.db.MapRegionEntity
import com.staticquo.maps.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject

data class DownloadMapsUiState(
    val availableRegions: List<MapRepository.AvailableRegion> = emptyList(),
    val downloadedRegions: List<MapRegionEntity> = emptyList(),
    val isLoading: Boolean = true,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadProgressText: String = "",
    val error: String? = null,
    val successMessage: String? = null,
    val locationQuery: String = "",
    val locationResults: List<GeocodingResult> = emptyList(),
    val isSearching: Boolean = false,
    val selectedLocation: GeocodingResult? = null,
    val minZoom: Int = 10,
    val maxZoom: Int = 14,
    val estimatedTiles: Long = 0
)

data class GeocodingResult(
    val displayName: String,
    val lat: Double,
    val lon: Double,
    val boundingBox: List<Double>?
)

@HiltViewModel
class DownloadMapsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mapRepository: MapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadMapsUiState())
    val uiState: StateFlow<DownloadMapsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = DownloadMapsUiState()
            try {
                val available = mapRepository.fetchAvailableRegions()
                val downloaded = mapRepository.getDownloadedRegions()
                _uiState.value = DownloadMapsUiState(
                    availableRegions = available,
                    downloadedRegions = downloaded
                )
            } catch (e: Exception) {
                _uiState.value = DownloadMapsUiState(
                    error = "Failed to load regions: ${e.message}"
                )
            }
        }
    }

    fun updateLocationQuery(query: String) {
        _uiState.value = _uiState.value.copy(locationQuery = query, selectedLocation = null)
    }

    fun searchLocation() {
        val query = _uiState.value.locationQuery.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true, error = null)
            try {
                val results = withContext(Dispatchers.IO) { geocode(query) }
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    locationResults = results
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    error = "Search failed: ${e.message}"
                )
            }
        }
    }

    fun selectLocation(result: GeocodingResult) {
        _uiState.value = _uiState.value.copy(
            selectedLocation = result,
            locationResults = emptyList(),
            locationQuery = result.displayName
        )
        recalcEstimate()
    }

    fun setMinZoom(z: Int) {
        _uiState.value = _uiState.value.copy(minZoom = z.coerceIn(5, _uiState.value.maxZoom))
        recalcEstimate()
    }

    fun setMaxZoom(z: Int) {
        _uiState.value = _uiState.value.copy(maxZoom = z.coerceIn(_uiState.value.minZoom, 18))
        recalcEstimate()
    }

    private fun recalcEstimate() {
        val sel = _uiState.value.selectedLocation ?: return
        val bb = sel.boundingBox
        val zooms = _uiState.value.minZoom.._uiState.value.maxZoom
        var count = 0L
        for (z in zooms) {
            if (bb != null && bb.size >= 4) {
                val (minT, maxT) = TileMath.tileBounds(bb[1], bb[3], bb[0], bb[2], z)
                count += (maxT.x - minT.x + 1).toLong() * (maxT.y - minT.y + 1).toLong()
            }
        }
        _uiState.value = _uiState.value.copy(estimatedTiles = count)
    }

    fun downloadCustomRegion() {
        val sel = _uiState.value.selectedLocation ?: return
        val bb = sel.boundingBox ?: return
        if (bb.size < 4) return

        viewModelScope.launch {
            val regionId = "custom-${sel.lat}-${sel.lon}".replace(".", "_")
            val sanName = sel.displayName.take(80)
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                downloadProgress = 0f,
                downloadProgressText = "Starting download...",
                error = null,
                successMessage = null
            )

            try {
                val regionsDir = File(context.filesDir, "maps")
                regionsDir.mkdirs()
                val outputFile = File(regionsDir, "$regionId.mbtiles")

                val builder = MbtilesBuilder()
                val result = withContext(Dispatchers.IO) {
                    builder.build(
                        outputFile = outputFile,
                        south = bb[1], north = bb[3],
                        west = bb[0], east = bb[2],
                        minZoom = _uiState.value.minZoom,
                        maxZoom = _uiState.value.maxZoom,
                        regionName = sanName
                    ) { progress ->
                        _uiState.value = _uiState.value.copy(
                            downloadProgress = progress.percentage,
                            downloadProgressText = progress.message
                        )
                    }
                }

                result.fold(
                    onSuccess = { file ->
                        mapRepository.saveRegion(
                            id = regionId,
                            name = sanName,
                            filePath = file.absolutePath,
                            sizeBytes = file.length(),
                            version = "1.0.0"
                        )
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            downloadProgress = 1f,
                            successMessage = "Custom region downloaded (${(file.length() / 1_000_000)} MB)"
                        )
                        refresh()
                    },
                    onFailure = { e ->
                        _uiState.value = _uiState.value.copy(
                            isDownloading = false,
                            error = "Download failed: ${e.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    error = "Download failed: ${e.message}"
                )
            }
        }
    }

    fun downloadRegion(region: MapRepository.AvailableRegion) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                downloadProgress = 0f,
                downloadProgressText = "Starting download...",
                error = null,
                successMessage = null
            )
            try {
                val regionsDir = File(context.filesDir, "maps")
                val file = mapRepository.downloadRegion(region, regionsDir) { progress ->
                    _uiState.value = _uiState.value.copy(
                        downloadProgress = progress,
                        downloadProgressText = "Downloading: ${(progress * 100).toInt()}%"
                    )
                }
                mapRepository.saveRegion(
                    id = region.id,
                    name = region.name,
                    filePath = file.absolutePath,
                    sizeBytes = file.length(),
                    version = region.version
                )
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadProgress = 1f,
                    successMessage = "${region.name} downloaded successfully."
                )
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    error = "Download failed: ${e.message}"
                )
            }
        }
    }

    fun removeRegion(region: MapRegionEntity) {
        viewModelScope.launch {
            mapRepository.removeRegion(region.id, File(region.mbtilesPath))
            refresh()
        }
    }

    private fun geocode(query: String): List<GeocodingResult> {
        val client = OkHttpClient()
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder()
            .url("https://nominatim.openstreetmap.org/search?q=$encodedQuery&format=json&limit=5")
            .header("User-Agent", "StaticQuo/1.0 (https://github.com/StaticQuo6/StaticQuo2)")
            .build()
        val response = client.newCall(request).execute()
        val jsonText = response.body?.string() ?: throw Exception("Empty response from Nominatim")
        response.close()
        val arr = org.json.JSONArray(jsonText)
        val results = mutableListOf<GeocodingResult>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val lat = obj.getDouble("lat")
            val lon = obj.getDouble("lon")
            val bbArr = obj.optJSONArray("boundingbox")
            val bb = if (bbArr != null && bbArr.length() >= 4) {
                listOf(bbArr.getDouble(2), bbArr.getDouble(0), bbArr.getDouble(3), bbArr.getDouble(1))
            } else null
            results.add(
                GeocodingResult(
                    displayName = obj.optString("display_name", ""),
                    lat = lat,
                    lon = lon,
                    boundingBox = bb
                )
            )
        }
        return results
    }
}

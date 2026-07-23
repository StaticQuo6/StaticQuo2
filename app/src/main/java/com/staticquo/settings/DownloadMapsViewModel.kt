package com.staticquo.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticquo.data.db.MapRegionEntity
import com.staticquo.maps.MapRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    val successMessage: String? = null
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
            _uiState.value = DownloadMapsUiState(isLoading = true)
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
}

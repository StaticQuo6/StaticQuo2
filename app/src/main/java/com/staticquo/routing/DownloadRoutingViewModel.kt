package com.staticquo.routing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.staticquo.data.db.RoutingRegionEntity
import javax.inject.Inject

data class DownloadRoutingUiState(
    val downloadedRegions: List<RoutingRegionEntity> = emptyList(),
    val availableRegions: List<RoutingRepository.AvailableRoutingRegion> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadProgressText: String = "",
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class DownloadRoutingViewModel @Inject constructor(
    private val repository: RoutingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadRoutingUiState())
    val uiState: StateFlow<DownloadRoutingUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = DownloadRoutingUiState(isInitialLoading = true)
            try {
                val available = repository.fetchAvailableRegions()
                val downloaded = repository.getDownloadedRegions()
                _uiState.value = DownloadRoutingUiState(
                    availableRegions = available,
                    downloadedRegions = downloaded,
                    isInitialLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = DownloadRoutingUiState(
                    error = "Failed to load: ${e.message}",
                    isInitialLoading = false
                )
            }
        }
    }

    fun downloadRegion(region: RoutingRepository.AvailableRoutingRegion) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                downloadProgress = 0f,
                downloadProgressText = "Downloading and extracting...",
                error = null,
                successMessage = null
            )
            try {
                val graphDir = withContext(Dispatchers.IO) {
                    repository.downloadAndExtract(region) { progress ->
                        _uiState.value = _uiState.value.copy(
                            downloadProgress = progress,
                            downloadProgressText = "Downloading: ${(progress * 100).toInt()}%"
                        )
                    }
                }

                var totalSize = 0L
                graphDir.walkTopDown().forEach { totalSize += it.length() }

                repository.saveRegion(
                    id = region.id,
                    name = region.name,
                    graphPath = graphDir.absolutePath,
                    sizeBytes = totalSize,
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

    fun removeRegion(region: RoutingRegionEntity) {
        viewModelScope.launch {
            repository.removeRegion(region.id)
            refresh()
        }
    }
}

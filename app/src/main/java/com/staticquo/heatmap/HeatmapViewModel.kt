package com.staticquo.heatmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HeatmapUiState(
    val beacons: List<HeatmapEntity> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val showLegend: Boolean = false,
    val showHeatmap: Boolean = true,
    val activeFilter: BeaconType? = null,
    val error: String? = null,
    val actionSuccess: String? = null
)

@HiltViewModel
class HeatmapViewModel @Inject constructor(
    private val repository: HeatmapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HeatmapUiState())
    val uiState: StateFlow<HeatmapUiState> = _uiState.asStateFlow()

    init {
        loadBeacons()
    }

    fun loadBeacons() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = repository.getAllBeacons()) {
                is HeatmapResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        beacons = result.data,
                        isLoading = false
                    )
                }
                is HeatmapResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun addBeacon(lat: Double, lng: Double, type: BeaconType, title: String, description: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showAddDialog = false)
            when (repository.addBeacon(lat, lng, type, title, description)) {
                is HeatmapResult.Success -> {
                    _uiState.value = _uiState.value.copy(actionSuccess = "Beacon placed")
                    loadBeacons()
                }
                is HeatmapResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = "Failed to place beacon")
                }
            }
        }
    }

    fun deleteBeacon(id: Long) {
        viewModelScope.launch {
            when (repository.deleteBeacon(id)) {
                is HeatmapResult.Success -> {
                    loadBeacons()
                }
                is HeatmapResult.Error -> {}
            }
        }
    }

    fun toggleLegend() {
        _uiState.value = _uiState.value.copy(showLegend = !_uiState.value.showLegend)
    }

    fun toggleHeatmap() {
        _uiState.value = _uiState.value.copy(showHeatmap = !_uiState.value.showHeatmap)
    }

    fun setFilter(type: BeaconType?) {
        _uiState.value = _uiState.value.copy(activeFilter = type)
    }

    fun clearActionSuccess() {
        _uiState.value = _uiState.value.copy(actionSuccess = null)
    }
}

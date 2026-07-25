package com.staticquo.routing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticquo.data.db.RoutingRegionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class RoutingUiState(
    val enabled: Boolean = false,
    val origin: Pair<Double, Double>? = null,
    val destination: Pair<Double, Double>? = null,
    val routePoints: List<Pair<Double, Double>> = emptyList(),
    val distanceMeters: Double = 0.0,
    val timeMillis: Long = 0,
    val isCalculating: Boolean = false,
    val profile: String = "car",
    val error: String? = null,
    val message: String? = null,
    val downloadedRegions: List<RoutingRegionEntity> = emptyList(),
    val activeRegionId: String? = null
)

@HiltViewModel
class RoutingViewModel @Inject constructor(
    private val repository: RoutingRepository,
    private val engine: GraphHopperEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutingUiState())
    val uiState: StateFlow<RoutingUiState> = _uiState.asStateFlow()

    init {
        loadRegions()
    }

    private fun loadRegions() {
        viewModelScope.launch {
            val regions = repository.getDownloadedRegions()
            _uiState.value = _uiState.value.copy(downloadedRegions = regions)
        }
    }

    fun toggleRouting() {
        val current = _uiState.value.enabled
        _uiState.value = RoutingUiState(
            enabled = !current,
            downloadedRegions = _uiState.value.downloadedRegions,
            activeRegionId = if (!current) _uiState.value.downloadedRegions.firstOrNull()?.id else null
        )
        if (!current) {
            autoLoadRegion()
        }
    }

    fun disableRouting() {
        _uiState.value = _uiState.value.copy(enabled = false, routePoints = emptyList(), origin = null, destination = null)
    }

    private fun autoLoadRegion() {
        viewModelScope.launch {
            if (engine.isLoaded()) return@launch
            val regions = repository.getDownloadedRegions()
            if (regions.isNotEmpty()) {
                setActiveRegion(regions.first().id)
            }
        }
    }

    fun setActiveRegion(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(activeRegionId = id, error = null)
            val result = repository.loadRegion(id)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        message = "Routing data loaded",
                        activeRegionId = id
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to load routing data: ${e.message}",
                        activeRegionId = null
                    )
                }
            )
        }
    }

    fun setOrigin(lat: Double, lon: Double) {
        _uiState.value = _uiState.value.copy(origin = Pair(lat, lon), routePoints = emptyList(), error = null)
    }

    fun setDestination(lat: Double? = null, lon: Double? = null) {
        val pair = if (lat != null && lon != null) Pair(lat, lon) else null
        _uiState.value = _uiState.value.copy(destination = pair, routePoints = emptyList(), error = null)
    }

    fun setProfile(profile: String) {
        _uiState.value = _uiState.value.copy(profile = profile)
    }

    fun calculateRoute() {
        val origin = _uiState.value.origin ?: return
        val dest = _uiState.value.destination ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCalculating = true, error = null, message = null)

            val result = withContext(Dispatchers.IO) {
                engine.route(
                    fromLat = origin.first, fromLon = origin.second,
                    toLat = dest.first, toLon = dest.second,
                    profile = _uiState.value.profile
                )
            }

            result.fold(
                onSuccess = { route ->
                    _uiState.value = _uiState.value.copy(
                        isCalculating = false,
                        routePoints = route.points,
                        distanceMeters = route.distanceMeters,
                        timeMillis = route.timeMillis,
                        message = "Route found (${formatDistance(route.distanceMeters)}, ${formatTime(route.timeMillis)})"
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isCalculating = false,
                        error = "Route calculation failed: ${e.message}"
                    )
                }
            )
        }
    }

    fun clearRoute() {
        _uiState.value = _uiState.value.copy(
            routePoints = emptyList(),
            origin = null,
            destination = null,
            distanceMeters = 0.0,
            timeMillis = 0,
            message = null,
            error = null
        )
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    private fun formatDistance(meters: Double): String {
        return if (meters >= 1000) "${"%.1f".format(meters / 1000)} km" else "${meters.toInt()} m"
    }

    private fun formatTime(millis: Long): String {
        val totalSec = millis / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        return if (h > 0) "${h}h ${m}min" else "${m}min"
    }

    override fun onCleared() {
        super.onCleared()
    }
}

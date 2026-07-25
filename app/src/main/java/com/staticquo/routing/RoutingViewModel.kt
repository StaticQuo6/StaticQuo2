// TODO: Reintroduce offline routing when a valhalla-mobile version compatible
// with Kotlin 2.0.x is available, or evaluate a different routing library.
// The repository methods now return Error stubs.
package com.staticquo.routing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RoutingUiState(
    val origin: RoutePoint? = null,
    val destination: RoutePoint? = null,
    val route: RouteInfo? = null,
    val isCalculating: Boolean = false,
    val error: String? = null,
    val hasTiles: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f
)

@HiltViewModel
class RoutingViewModel @Inject constructor(
    private val repository: RoutingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutingUiState())
    val uiState: StateFlow<RoutingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.installDemoIfNeeded()
            _uiState.value = _uiState.value.copy(hasTiles = repository.hasTiles())
        }
    }

    fun setOrigin(point: RoutePoint) {
        _uiState.value = _uiState.value.copy(
            origin = point,
            route = null,
            error = null
        )
        autoCalculate()
    }

    fun setDestination(point: RoutePoint) {
        _uiState.value = _uiState.value.copy(
            destination = point,
            route = null,
            error = null
        )
        autoCalculate()
    }

    fun clearRoute() {
        _uiState.value = _uiState.value.copy(
            route = null,
            error = null
        )
    }

    fun clearPoints() {
        _uiState.value = _uiState.value.copy(
            origin = null,
            destination = null,
            route = null,
            error = null
        )
    }

    fun downloadTiles(url: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                downloadProgress = 0f,
                error = null
            )
            when (val result = repository.downloadTiles(url) { progress ->
                _uiState.value = _uiState.value.copy(downloadProgress = progress)
            }) {
                is RoutingResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        hasTiles = true,
                        downloadProgress = 1f
                    )
                }
                is RoutingResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        error = result.message,
                        downloadProgress = 0f
                    )
                }
            }
        }
    }

    private fun autoCalculate() {
        val origin = _uiState.value.origin ?: return
        val dest = _uiState.value.destination ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCalculating = true, error = null)
            when (val result = repository.route(origin, dest)) {
                is RoutingResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        route = result.data,
                        isCalculating = false
                    )
                }
                is RoutingResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.message,
                        isCalculating = false
                    )
                }
            }
        }
    }
}

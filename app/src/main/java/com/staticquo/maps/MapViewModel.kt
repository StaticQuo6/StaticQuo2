package com.staticquo.maps

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.staticquo.data.db.MapRegionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

data class MapUiState(
    val activeRegion: MapRegionEntity? = null,
    val userLocation: Location? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class MapViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mapRepository: MapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val region = mapRepository.getActiveRegion()
            _uiState.value = MapUiState(
                activeRegion = region,
                isLoading = false
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun requestLocation() {
        viewModelScope.launch {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val location = fusedClient.lastLocation.await()
                if (location != null) {
                    _uiState.value = _uiState.value.copy(userLocation = location)
                }
            } catch (_: SecurityException) {
                _uiState.value = _uiState.value.copy(
                    error = "Location permission not granted"
                )
            }
        }
    }

    fun refreshRegion() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val region = mapRepository.getActiveRegion()
            _uiState.value = _uiState.value.copy(
                activeRegion = region,
                isLoading = false
            )
        }
    }
}

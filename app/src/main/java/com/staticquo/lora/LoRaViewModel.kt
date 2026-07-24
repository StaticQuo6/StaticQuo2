package com.staticquo.lora

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoRaUiState(
    val isConnected: Boolean = false,
    val deviceInfo: String = "",
    val packets: List<LoRaPacket> = emptyList(),
    val frequency: Double = LoRaConstants.DEFAULT_FREQUENCY_MHZ,
    val spreadingFactor: Int = LoRaConstants.DEFAULT_SPREADING_FACTOR,
    val isScanning: Boolean = false,
    val statusMessage: String = "LoRa module not connected"
)

@HiltViewModel
class LoRaViewModel @Inject constructor(
    private val repository: LoRaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoRaUiState())
    val uiState: StateFlow<LoRaUiState> = _uiState.asStateFlow()

    private var receiveJob: kotlinx.coroutines.Job? = null

    fun scanForDevice() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            delay(500)
            when (val result = repository.findDevice()) {
                is LoRaResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        deviceInfo = result.data,
                        isScanning = false
                    )
                }
                is LoRaResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = result.message,
                        isScanning = false
                    )
                }
            }
        }
    }

    fun connect() {
        viewModelScope.launch {
            when (val result = repository.connect()) {
                is LoRaResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isConnected = true,
                        statusMessage = "Connected to LoRa module"
                    )
                    startReceiveLoop()
                }
                is LoRaResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = result.message
                    )
                }
            }
        }
    }

    fun disconnect() {
        receiveJob?.cancel()
        repository.disconnect()
        _uiState.value = _uiState.value.copy(
            isConnected = false,
            statusMessage = "Disconnected"
        )
    }

    fun sendMessage(content: String) {
        if (!_uiState.value.isConnected) return
        viewModelScope.launch {
            when (repository.sendMessage(content)) {
                is LoRaResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        packets = repository.getReceivedPackets()
                    )
                }
                is LoRaResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "Send failed"
                    )
                }
            }
        }
    }

    fun setFrequency(freq: Double) {
        repository.setFrequency(freq)
        _uiState.value = _uiState.value.copy(frequency = freq)
    }

    fun setSpreadingFactor(sf: Int) {
        repository.setSpreadingFactor(sf)
        _uiState.value = _uiState.value.copy(spreadingFactor = sf)
    }

    private fun startReceiveLoop() {
        receiveJob = viewModelScope.launch {
            while (isActive) {
                repository.tryReceive()
                _uiState.value = _uiState.value.copy(
                    packets = repository.getReceivedPackets()
                )
                delay(2000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }
}

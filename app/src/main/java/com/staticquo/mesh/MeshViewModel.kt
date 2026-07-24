package com.staticquo.mesh

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MeshUiState(
    val isStarted: Boolean = false,
    val peers: List<PeerInfo> = emptyList(),
    val messages: List<MeshMessage> = emptyList(),
    val statusMessage: String = "",
    val error: String? = null,
    val initResult: MeshInitResult? = null
)

@HiltViewModel
class MeshViewModel @Inject constructor(
    private val meshRepository: MeshRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeshUiState())
    val uiState: StateFlow<MeshUiState> = _uiState.asStateFlow()

    fun startMesh() {
        viewModelScope.launch {
            val result = meshRepository.initialize()
            _uiState.value = _uiState.value.copy(initResult = result)

            when (result) {
                is MeshInitResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isStarted = true,
                        statusMessage = "Mesh active"
                    )
                    startPolling()
                }
                is MeshInitResult.PermissionsDenied -> {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "Missing: ${result.missing.joinToString(", ")}"
                    )
                }
                is MeshInitResult.BluetoothNotAvailable -> {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = result.detail
                    )
                }
                is MeshInitResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "Error: ${result.throwable.message}"
                    )
                }
            }
        }
    }

    fun stopMesh() {
        meshRepository.shutdown()
        _uiState.value = MeshUiState()
    }

    fun sendMessage(content: String) {
        if (!_uiState.value.isStarted) return
        meshRepository.sendMessage(content)
    }

    override fun onCleared() {
        super.onCleared()
        meshRepository.shutdown()
    }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                _uiState.value = _uiState.value.copy(
                    peers = meshRepository.getPeers(),
                    messages = meshRepository.getMessages()
                )
                delay(2000)
            }
        }
    }
}

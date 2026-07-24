package com.staticquo.backup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BackupUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val isConfigured: Boolean = false,
    val isBusy: Boolean = false,
    val statusMessage: String = "",
    val lastBackupFiles: List<String> = emptyList()
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: BackupRepository
) : ViewModel() {

    private val prefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        val savedUrl = prefs.getString("url", "") ?: ""
        val savedUser = prefs.getString("username", "") ?: ""
        val savedPass = prefs.getString("password", "") ?: ""

        if (savedUrl.isNotBlank()) {
            repository.configure(savedUrl, savedUser, savedPass)
            _uiState.value = _uiState.value.copy(
                serverUrl = savedUrl,
                username = savedUser,
                password = savedPass,
                isConfigured = true
            )
        }
    }

    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(serverUrl = url)
    }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun saveAndTest() {
        val state = _uiState.value
        if (state.serverUrl.isBlank()) return

        prefs.edit()
            .putString("url", state.serverUrl)
            .putString("username", state.username)
            .putString("password", state.password)
            .apply()

        repository.configure(state.serverUrl, state.username, state.password)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, statusMessage = "Testing connection...")
            when (repository.testConnection()) {
                is BackupResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isConfigured = true,
                        isBusy = false,
                        statusMessage = "Connected to server"
                    )
                    refreshBackupList()
                }
                is BackupResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isConfigured = false,
                        isBusy = false,
                        statusMessage = "Connection failed"
                    )
                }
            }
        }
    }

    fun performBackup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, statusMessage = "Backing up...")
            when (val result = repository.performBackup { msg ->
                _uiState.value = _uiState.value.copy(statusMessage = msg)
            }) {
                is BackupResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
                        statusMessage = result.data
                    )
                    refreshBackupList()
                }
                is BackupResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
                        statusMessage = result.message
                    )
                }
            }
        }
    }

    fun performRestore() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBusy = true, statusMessage = "Restoring...")
            when (val result = repository.performRestore { msg ->
                _uiState.value = _uiState.value.copy(statusMessage = msg)
            }) {
                is BackupResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
                        statusMessage = result.data
                    )
                }
                is BackupResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isBusy = false,
                        statusMessage = result.message
                    )
                }
            }
        }
    }

    private fun refreshBackupList() {
        viewModelScope.launch {
            when (val result = repository.listBackups()) {
                is BackupResult.Success -> {
                    _uiState.value = _uiState.value.copy(lastBackupFiles = result.data)
                }
                is BackupResult.Error -> {}
            }
        }
    }
}

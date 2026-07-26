package com.staticquo.vault

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticquo.data.db.VaultEntryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class VaultLockScreen {
    LOADING,
    SET_PIN,
    UNLOCK,
    CONTENT
}

data class VaultUiState(
    val lockScreen: VaultLockScreen = VaultLockScreen.LOADING,
    val pinEntry: String = "",
    val pinError: String? = null,
    val lockoutRemainingMs: Long = 0,
    val firstPin: String = "",
    val entries: List<VaultEntryEntity> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedEntry: DecryptedEntry? = null,
    val showAddDialog: Boolean = false,
    val showEntryDialog: Boolean = false,
    val actionSuccess: String? = null
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: VaultRepository,
    private val vaultPinRepository: VaultPinRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    fun lock() {
        _uiState.value = _uiState.value.copy(lockScreen = VaultLockScreen.LOADING)
        viewModelScope.launch {
            val isSet = vaultPinRepository.isPinSet()
            _uiState.value = _uiState.value.copy(
                lockScreen = if (isSet) VaultLockScreen.UNLOCK else VaultLockScreen.SET_PIN
            )
        }
    }

    init {
        lock()
    }

    fun onPinDigit(digit: Char) {
        val current = _uiState.value.pinEntry
        if (current.length < 8) {
            _uiState.value = _uiState.value.copy(pinEntry = current + digit, pinError = null)
        }
    }

    fun onDeleteDigit() {
        val current = _uiState.value.pinEntry
        if (current.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(pinEntry = current.dropLast(1), pinError = null)
        }
    }

    fun onSetupPin(pin: String) {
        val cur = _uiState.value
        if (cur.firstPin.isEmpty()) {
            if (pin.length < 4) {
                _uiState.value = cur.copy(pinError = "PIN must be at least 4 digits")
                return
            }
            _uiState.value = cur.copy(pinEntry = "", firstPin = pin, pinError = "Confirm your PIN")
        } else {
            if (pin == cur.firstPin) {
                viewModelScope.launch {
                    vaultPinRepository.setPin(pin)
                    _uiState.value = _uiState.value.copy(
                        lockScreen = VaultLockScreen.CONTENT,
                        pinEntry = "",
                        firstPin = "",
                        pinError = null
                    )
                    loadEntries()
                }
            } else {
                _uiState.value = cur.copy(pinEntry = "", firstPin = "", pinError = "PINs do not match. Try again.")
            }
        }
    }

    fun onUnlockPin(pin: String) {
        viewModelScope.launch {
            when (val result = vaultPinRepository.verifyPin(pin)) {
                VaultPinRepository.VaultPinResult.CORRECT -> {
                    _uiState.value = _uiState.value.copy(lockScreen = VaultLockScreen.CONTENT, pinEntry = "", pinError = null)
                    loadEntries()
                }
                VaultPinRepository.VaultPinResult.INCORRECT -> {
                    val attempts = vaultPinRepository.getFailedAttempts()
                    _uiState.value = _uiState.value.copy(
                        pinEntry = "",
                        pinError = if (attempts >= 5) {
                            "Wrong PIN. Lockout will activate after $attempts/5 attempts."
                        } else {
                            "Wrong PIN. $attempts of 5 attempts used."
                        }
                    )
                }
                VaultPinRepository.VaultPinResult.LOCKED_OUT -> {
                    _uiState.value = _uiState.value.copy(
                        pinEntry = "",
                        pinError = "Too many attempts. Vault locked."
                    )
                }
            }
        }
    }

    fun loadEntries() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getAllEntries()) {
                is VaultResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        entries = result.data,
                        isLoading = false
                    )
                }
                is VaultResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun importFile(uri: Uri, fileName: String, mimeType: String, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val data = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: throw Exception("Cannot read file")
                }
                if (data.size > 50 * 1024 * 1024) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "File too large (max 50 MB)"
                    )
                    return@launch
                }
                val title = fileName.substringBeforeLast(".")
                when (val result = repository.createEntry(title, mimeType, data)) {
                    is VaultResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            actionSuccess = "File imported: $fileName"
                        )
                        loadEntries()
                    }
                    is VaultResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Import failed: ${e.message}"
                )
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true, actionSuccess = null)
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false)
    }

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(showAddDialog = false)
            when (val result = repository.createNote(title, content)) {
                is VaultResult.Success -> {
                    _uiState.value = _uiState.value.copy(actionSuccess = "Note saved")
                    loadEntries()
                }
                is VaultResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
            }
        }
    }

    fun viewEntry(id: Long) {
        viewModelScope.launch {
            when (val result = repository.getEntry(id)) {
                is VaultResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        selectedEntry = result.data,
                        showEntryDialog = true
                    )
                }
                is VaultResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
            }
        }
    }

    fun hideEntryDialog() {
        _uiState.value = _uiState.value.copy(showEntryDialog = false, selectedEntry = null)
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch {
            when (repository.deleteEntry(id)) {
                is VaultResult.Success -> {
                    _uiState.value = _uiState.value.copy(actionSuccess = "Entry deleted")
                    loadEntries()
                }
                is VaultResult.Error -> {
                    _uiState.value = _uiState.value.copy(error = "Delete failed")
                }
            }
        }
    }

    fun clearActionSuccess() {
        _uiState.value = _uiState.value.copy(actionSuccess = null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

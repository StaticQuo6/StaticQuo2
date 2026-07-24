package com.staticquo.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.staticquo.data.db.VaultEntryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultUiState(
    val entries: List<VaultEntryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedEntry: DecryptedEntry? = null,
    val showAddDialog: Boolean = false,
    val showEntryDialog: Boolean = false,
    val actionSuccess: String? = null
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repository: VaultRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        loadEntries()
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

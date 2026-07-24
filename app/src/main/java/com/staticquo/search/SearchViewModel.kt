package com.staticquo.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResultItem> = emptyList(),
    val isIndexing: Boolean = true,
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val selectedResult: SearchResultItem? = null,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureIndexed()
            _uiState.value = _uiState.value.copy(isIndexing = false)
        }
    }

    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
    }

    fun search() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearching = true)
            when (val result = repository.search(query)) {
                is SearchResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        results = result.data,
                        isSearching = false,
                        hasSearched = true,
                        error = null
                    )
                }
                is SearchResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun selectResult(result: SearchResultItem) {
        _uiState.value = _uiState.value.copy(selectedResult = result)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedResult = null)
    }
}

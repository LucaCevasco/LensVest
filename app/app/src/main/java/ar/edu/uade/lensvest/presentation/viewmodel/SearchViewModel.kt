package ar.edu.uade.lensvest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.lensvest.domain.model.AssetSearchResult
import ar.edu.uade.lensvest.domain.usecase.SearchAssetsUseCase
import ar.edu.uade.lensvest.mapper.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** D3 · Buscar a mano: when the camera can't tell, the user types the brand or company. */
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchAssetsUseCase: SearchAssetsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState = _uiState.asStateFlow()
    private var search: Job? = null

    fun onQueryChange(query: String) = _uiState.update { it.copy(query = query) }

    /** Each search is an AI call on Berry (~10 s), so it runs on submit rather than per keystroke. */
    fun submit() {
        val query = _uiState.value.query.trim()
        if (query.length < 2) return
        search?.cancel()
        search = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, searched = query) }
            try {
                val results = searchAssetsUseCase(query)
                _uiState.update { it.copy(results = results, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage()) }
            }
        }
    }
}

data class SearchUiState(
    val query: String = "",
    /** The query the results belong to. */
    val searched: String? = null,
    val results: List<AssetSearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

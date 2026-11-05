package ar.edu.uade.lensvest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.lensvest.domain.model.Quote
import ar.edu.uade.lensvest.domain.model.Scan
import ar.edu.uade.lensvest.domain.usecase.GetQuotesUseCase
import ar.edu.uade.lensvest.domain.usecase.GetScanHistoryUseCase
import ar.edu.uade.lensvest.domain.usecase.SetScanFavoriteUseCase
import ar.edu.uade.lensvest.mapper.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 09 · Tus escaneos. */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getScanHistoryUseCase: GetScanHistoryUseCase,
    private val getQuotesUseCase: GetQuotesUseCase,
    private val setScanFavoriteUseCase: SetScanFavoriteUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val scans = getScanHistoryUseCase()
                _uiState.update { it.copy(scans = scans, isLoading = false) }
                loadQuotes(scans)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage()) }
            }
        }
    }

    private suspend fun loadQuotes(scans: List<Scan>) {
        try {
            val quotes = getQuotesUseCase(scans.mapNotNull { it.ticker })
            _uiState.update { it.copy(quotes = quotes.associateBy { q -> q.ticker }) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Prices are extra: the list still works without them.
        }
    }

    fun showFavorites(favorites: Boolean) = _uiState.update { it.copy(showFavorites = favorites) }

    fun toggleFavorite(scan: Scan) {
        viewModelScope.launch {
            try {
                val updated = setScanFavoriteUseCase(scan.id, !scan.favorite)
                _uiState.update { state -> state.copy(scans = state.scans.map { if (it.id == updated.id) updated else it }) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUserMessage()) }
            }
        }
    }
}

data class HistoryUiState(
    val scans: List<Scan> = emptyList(),
    val quotes: Map<String, Quote> = emptyMap(),
    val showFavorites: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val visible get() = if (showFavorites) scans.filter { it.favorite } else scans
    val favoriteCount get() = scans.count { it.favorite }
}

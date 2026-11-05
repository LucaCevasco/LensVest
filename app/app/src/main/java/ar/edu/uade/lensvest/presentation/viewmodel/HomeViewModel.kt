package ar.edu.uade.lensvest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.lensvest.domain.model.Portfolio
import ar.edu.uade.lensvest.domain.model.Strategy
import ar.edu.uade.lensvest.domain.usecase.GetBerryStatusUseCase
import ar.edu.uade.lensvest.domain.usecase.GetPortfolioUseCase
import ar.edu.uade.lensvest.domain.usecase.GetStrategiesUseCase
import ar.edu.uade.lensvest.domain.usecase.GetUserNameUseCase
import ar.edu.uade.lensvest.mapper.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    getUserNameUseCase: GetUserNameUseCase,
    private val getBerryStatusUseCase: GetBerryStatusUseCase,
    private val getPortfolioUseCase: GetPortfolioUseCase,
    private val getStrategiesUseCase: GetStrategiesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(userName = getUserNameUseCase().orEmpty()))
    val uiState = _uiState.asStateFlow()

    /** Called when Home is shown, so a connection or an order made elsewhere shows up. */
    fun refresh() {
        viewModelScope.launch {
            try {
                val connected = getBerryStatusUseCase().connected
                _uiState.update { it.copy(isBerryConnected = connected, error = null) }
                if (connected) {
                    launch { loadStrategies() }
                    loadPortfolio()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUserMessage()) }
            }
        }
    }

    fun retryPortfolio() {
        viewModelScope.launch { loadPortfolio() }
    }

    private suspend fun loadPortfolio() {
        // Keep showing the last portfolio while the new one loads.
        _uiState.update { it.copy(isPortfolioLoading = true, portfolioError = null) }
        try {
            val portfolio = getPortfolioUseCase()
            _uiState.update { it.copy(portfolio = portfolio, isPortfolioLoading = false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _uiState.update { it.copy(isPortfolioLoading = false, portfolioError = e.toUserMessage()) }
        }
    }

    private suspend fun loadStrategies() {
        try {
            val active = getStrategiesUseCase().filter { it.isActive }
            _uiState.update { it.copy(activeStrategies = active) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Secondary on Home: the Estrategias tab shows the error with a retry.
        }
    }
}

data class HomeUiState(
    val userName: String = "",
    /** Null until the first status check finishes. */
    val isBerryConnected: Boolean? = null,
    val portfolio: Portfolio? = null,
    val isPortfolioLoading: Boolean = false,
    val portfolioError: String? = null,
    val activeStrategies: List<Strategy> = emptyList(),
    val error: String? = null,
)

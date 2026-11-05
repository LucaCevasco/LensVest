package ar.edu.uade.lensvest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.lensvest.domain.model.Strategy
import ar.edu.uade.lensvest.domain.usecase.DeleteStrategyUseCase
import ar.edu.uade.lensvest.domain.usecase.GetStrategiesUseCase
import ar.edu.uade.lensvest.domain.usecase.SetStrategyActiveUseCase
import ar.edu.uade.lensvest.mapper.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** F1 · Estrategias: list, pause, resume and delete (RF04). */
@HiltViewModel
class StrategiesViewModel @Inject constructor(
    private val getStrategiesUseCase: GetStrategiesUseCase,
    private val setStrategyActiveUseCase: SetStrategyActiveUseCase,
    private val deleteStrategyUseCase: DeleteStrategyUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StrategiesUiState())
    val uiState = _uiState.asStateFlow()

    fun load() = run(showLoading = true) {
        val strategies = getStrategiesUseCase()
        _uiState.update { it.copy(strategies = strategies, hasLoaded = true) }
    }

    fun setActive(strategy: Strategy, active: Boolean) = run {
        val updated = setStrategyActiveUseCase(strategy.id, active)
        _uiState.update { state -> state.copy(strategies = state.strategies.map { if (it.id == updated.id) updated else it }) }
    }

    fun delete(strategy: Strategy) = run {
        deleteStrategyUseCase(strategy.id)
        _uiState.update { state -> state.copy(strategies = state.strategies.filterNot { it.id == strategy.id }) }
    }

    private fun run(showLoading: Boolean = false, action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = showLoading || it.isLoading, error = null) }
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUserMessage()) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}

data class StrategiesUiState(
    val strategies: List<Strategy> = emptyList(),
    val isLoading: Boolean = false,
    val hasLoaded: Boolean = false,
    val error: String? = null,
) {
    val activeCount get() = strategies.count { it.isActive }
    val pausedCount get() = strategies.size - activeCount
}

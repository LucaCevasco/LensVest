package ar.edu.uade.lensvest.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.lensvest.domain.model.OrderResult
import ar.edu.uade.lensvest.domain.usecase.ConfirmOrderUseCase
import ar.edu.uade.lensvest.domain.usecase.DiscardOrderUseCase
import ar.edu.uade.lensvest.mapper.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** 07 · Revisión: nothing reaches Berry until the user confirms here (RNF01). */
@HiltViewModel
class ReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val confirmOrderUseCase: ConfirmOrderUseCase,
    private val discardOrderUseCase: DiscardOrderUseCase,
) : ViewModel() {

    val ticker: String = checkNotNull(savedStateHandle["ticker"])
    val name: String = savedStateHandle["name"] ?: ticker
    val isSell: Boolean = savedStateHandle.get<String>("side") == "sell"
    val amount: Double = savedStateHandle.get<String>("amount")?.toDoubleOrNull() ?: 0.0
    val amountUnit: String = savedStateHandle["amountUnit"] ?: "USD"
    val priceUsd: Double? = savedStateHandle.get<String>("priceUsd")?.toDoubleOrNull()

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState = _uiState.asStateFlow()

    /** Shares the amount buys at the last price; the real fill can differ a little. */
    val estimatedShares: Double? get() = priceUsd?.takeIf { it > 0 && amountUnit == "USD" }?.let { amount / it }

    fun confirm() {
        if (_uiState.value.isSending) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            try {
                val result = confirmOrderUseCase()
                _uiState.update {
                    // RNF06: only Berry's answer marks the order as done.
                    if (result.executed) it.copy(isSending = false, result = result)
                    else it.copy(isSending = false, error = result.message, isSettled = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isSending = false, error = e.toUserMessage()) }
            }
        }
    }

    /** Leaving without confirming rejects the order, so the assistant stops waiting for it. */
    fun discard() {
        val state = _uiState.value
        if (state.result != null || state.isSettled || state.isSending) return
        viewModelScope.launch {
            withContext(NonCancellable) { runCatching { discardOrderUseCase() } }
        }
    }
}

data class ReviewUiState(
    val isSending: Boolean = false,
    val result: OrderResult? = null,
    val error: String? = null,
    /** Berry already answered (rejected it): there's nothing left to discard or retry. */
    val isSettled: Boolean = false,
)

package ar.edu.uade.lensvest.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.lensvest.domain.model.OrderSide
import ar.edu.uade.lensvest.domain.model.PreparedOrder
import ar.edu.uade.lensvest.domain.usecase.PrepareOrderUseCase
import ar.edu.uade.lensvest.mapper.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Longest amount the keypad accepts, so the number fits on screen. */
private const val MAX_DIGITS = 7

/** 06 · Monto: the user types how much, and the assistant prepares the order on Berry. */
@HiltViewModel
class AmountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val prepareOrderUseCase: PrepareOrderUseCase,
) : ViewModel() {

    val ticker: String = checkNotNull(savedStateHandle["ticker"])
    val name: String = savedStateHandle["name"] ?: ticker
    val side: OrderSide = if (savedStateHandle.get<String>("side") == "sell") OrderSide.SELL else OrderSide.BUY
    val priceUsd: Double? = savedStateHandle.get<String>("priceUsd")?.toDoubleOrNull()
    /** Cash to buy with, or what the position is worth when selling. */
    val availableUsd: Double? = savedStateHandle.get<String>("availableUsd")?.toDoubleOrNull()

    // Starts on a preset the user can afford.
    private val _uiState = MutableStateFlow(AmountUiState(text = if ((availableUsd ?: 5.0) < 5) "1" else "5"))
    val uiState = _uiState.asStateFlow()

    fun preset(amount: Int) = _uiState.update { it.copy(text = amount.toString(), error = null) }

    /** "Otro": clears the amount so the user types their own. */
    fun clear() = _uiState.update { it.copy(text = "", error = null) }

    fun type(key: Char) = _uiState.update { state ->
        val text = state.text
        val next = when {
            key == ',' && (text.contains(',') || text.isEmpty()) -> text
            key == ',' -> "$text,"
            text.substringAfter(',', "").length >= 2 && text.contains(',') -> text
            text.count { it.isDigit() } >= MAX_DIGITS -> text
            text == "0" -> key.toString()
            else -> text + key
        }
        state.copy(text = next, error = null)
    }

    fun backspace() = _uiState.update { it.copy(text = it.text.dropLast(1), error = null) }

    fun review() {
        val amount = _uiState.value.amount
        when {
            amount == null || amount <= 0 -> return
            availableUsd != null && amount > availableUsd -> _uiState.update {
                it.copy(error = if (side == OrderSide.SELL) "Tenés menos que eso en $ticker." else "No te alcanza: tenés menos disponible en Berry.")
            }
            else -> viewModelScope.launch {
                _uiState.update { it.copy(isPreparing = true, error = null) }
                try {
                    val order = prepareOrderUseCase(ticker, side, amount)
                    _uiState.update { it.copy(isPreparing = false, prepared = order) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _uiState.update { it.copy(isPreparing = false, error = e.toUserMessage()) }
                }
            }
        }
    }

    fun onReviewOpened() = _uiState.update { it.copy(prepared = null) }
}

data class AmountUiState(
    /** What the keypad typed, with a decimal comma: "5", "12,5". */
    val text: String,
    val isPreparing: Boolean = false,
    val error: String? = null,
    /** Set once the order is waiting on Berry; the screen moves on to review it. */
    val prepared: PreparedOrder? = null,
) {
    val amount: Double? get() = text.replace(',', '.').toDoubleOrNull()
}

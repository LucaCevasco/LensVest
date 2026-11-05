package ar.edu.uade.lensvest.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.lensvest.domain.model.AssetDetail
import ar.edu.uade.lensvest.domain.model.SheetMode
import ar.edu.uade.lensvest.domain.usecase.GetAssetDetailUseCase
import ar.edu.uade.lensvest.domain.usecase.ObservePlainExplanationsUseCase
import ar.edu.uade.lensvest.domain.usecase.ObserveSheetModeUseCase
import ar.edu.uade.lensvest.domain.usecase.SetScanFavoriteUseCase
import ar.edu.uade.lensvest.domain.usecase.SetSheetModeUseCase
import ar.edu.uade.lensvest.mapper.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 05 / 05b / 05c · Ficha de la empresa. */
@HiltViewModel
class AssetViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAssetDetailUseCase: GetAssetDetailUseCase,
    private val setSheetModeUseCase: SetSheetModeUseCase,
    private val setScanFavoriteUseCase: SetScanFavoriteUseCase,
    observeSheetModeUseCase: ObserveSheetModeUseCase,
    observePlainExplanationsUseCase: ObservePlainExplanationsUseCase,
) : ViewModel() {

    private val ticker: String = checkNotNull(savedStateHandle["ticker"])
    /** The scanned brand, when the sheet opens from Lens Mode (e.g. "Sprite" for KO). */
    private val brand: String? = savedStateHandle["brand"]
    private val scanId: Long? = savedStateHandle.get<Long>("scanId")?.takeIf { it > 0 }

    private val content = MutableStateFlow(
        AssetContent(ticker = ticker, brand = brand, favorite = savedStateHandle["favorite"] ?: false, canFavorite = scanId != null),
    )

    val uiState = combine(content, observeSheetModeUseCase(), observePlainExplanationsUseCase()) { c, mode, plain ->
        AssetUiState(c, mode, plain)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AssetUiState(content.value, SheetMode.SIMPLE, true))

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            content.update { it.copy(isLoading = true, error = null) }
            try {
                val asset = getAssetDetailUseCase(ticker)
                content.update { it.copy(asset = asset, isLoading = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                content.update { it.copy(isLoading = false, error = e.toUserMessage()) }
            }
        }
    }

    fun setMode(mode: SheetMode) = setSheetModeUseCase(mode)

    fun toggleFavorite() {
        val id = scanId ?: return
        val favorite = !content.value.favorite
        content.update { it.copy(favorite = favorite) }
        viewModelScope.launch {
            try {
                setScanFavoriteUseCase(id, favorite)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                content.update { it.copy(favorite = !favorite, error = e.toUserMessage()) }
            }
        }
    }
}

data class AssetContent(
    val ticker: String,
    val brand: String?,
    val asset: AssetDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val favorite: Boolean = false,
    /** Only scans can be favorites. */
    val canFavorite: Boolean = false,
)

data class AssetUiState(val content: AssetContent, val mode: SheetMode, val plainExplanations: Boolean)

package ar.edu.uade.lensvest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.lensvest.domain.model.BerryStatus
import ar.edu.uade.lensvest.domain.usecase.CompleteBerryConnectionUseCase
import ar.edu.uade.lensvest.domain.usecase.GetBerryStatusUseCase
import ar.edu.uade.lensvest.domain.usecase.StartBerryConnectionUseCase
import ar.edu.uade.lensvest.mapper.toUserMessage
import ar.edu.uade.lensvest.presentation.ui.home.BerryCallback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getBerryStatusUseCase: GetBerryStatusUseCase,
    private val startBerryConnectionUseCase: StartBerryConnectionUseCase,
    private val completeBerryConnectionUseCase: CompleteBerryConnectionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshStatus()
    }

    fun refreshStatus() = load { showStatus(getBerryStatusUseCase()) }

    fun connectBerry() = load {
        val url = startBerryConnectionUseCase()
        _uiState.update { it.copy(authorizeUrl = url) }
    }

    fun onAuthorizeUrlOpened() = _uiState.update { it.copy(authorizeUrl = null) }

    fun onBerryCallback(callback: BerryCallback) = when (callback) {
        is BerryCallback.Failed -> _uiState.update { it.copy(error = callback.message) }
        is BerryCallback.Authorized -> load {
            showStatus(completeBerryConnectionUseCase(callback.code, callback.state))
        }
    }

    private fun showStatus(status: BerryStatus) =
        _uiState.update { it.copy(isBerryConnected = status.connected) }

    private fun load(action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
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

data class HomeUiState(
    val isBerryConnected: Boolean = false,
    /** One-shot: the screen opens it in the browser and calls [HomeViewModel.onAuthorizeUrlOpened]. */
    val authorizeUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

package ar.edu.uade.lensvest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.lensvest.domain.usecase.GetBerryStatusUseCase
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(userName = getUserNameUseCase().orEmpty()))
    val uiState = _uiState.asStateFlow()

    /** Called when Home is shown, so a connection made elsewhere shows up. */
    fun refresh() {
        viewModelScope.launch {
            try {
                val status = getBerryStatusUseCase()
                _uiState.update { it.copy(isBerryConnected = status.connected, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.toUserMessage()) }
            }
        }
    }
}

data class HomeUiState(
    val userName: String = "",
    /** Null until the first status check finishes. */
    val isBerryConnected: Boolean? = null,
    val error: String? = null,
)

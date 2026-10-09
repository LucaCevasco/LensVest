package ar.edu.uade.lensvest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.lensvest.domain.model.AssistantTurn
import ar.edu.uade.lensvest.domain.model.ChatMessage
import ar.edu.uade.lensvest.domain.model.ChatRole
import ar.edu.uade.lensvest.domain.model.PendingAction
import ar.edu.uade.lensvest.domain.usecase.ResolvePendingActionsUseCase
import ar.edu.uade.lensvest.domain.usecase.SendAssistantMessageUseCase
import ar.edu.uade.lensvest.mapper.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val sendAssistantMessageUseCase: SendAssistantMessageUseCase,
    private val resolvePendingActionsUseCase: ResolvePendingActionsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState = _uiState.asStateFlow()

    fun send(text: String) {
        if (text.isBlank() || _uiState.value.isLoading) return
        _uiState.update {
            // Sending a new message rejects any order still waiting (the backend does the same).
            it.copy(messages = it.messages + ChatMessage(ChatRole.USER, text.trim()), pendingActions = emptyList())
        }
        respond { sendAssistantMessageUseCase(text) }
    }

    fun confirmPendingActions() = respond { resolvePendingActionsUseCase(approve = true) }

    fun rejectPendingActions() = respond { resolvePendingActionsUseCase(approve = false) }

    private fun respond(call: suspend () -> AssistantTurn) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val turn = call()
                _uiState.update { state ->
                    val reply = turn.reply.takeIf { it.isNotBlank() }?.let { ChatMessage(ChatRole.ASSISTANT, it) }
                    state.copy(
                        messages = state.messages + listOfNotNull(reply),
                        pendingActions = turn.pendingActions,
                        isLoading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toUserMessage()) }
            }
        }
    }
}

data class AssistantUiState(
    // ponytail: history lives only in memory; the server session keeps the full transcript.
    val messages: List<ChatMessage> = emptyList(),
    /** Operations the user must confirm before they reach Berry (RNF01). */
    val pendingActions: List<PendingAction> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

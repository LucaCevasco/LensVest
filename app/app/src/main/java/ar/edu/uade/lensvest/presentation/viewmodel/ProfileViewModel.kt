package ar.edu.uade.lensvest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.uade.lensvest.domain.model.InvestorLevel
import ar.edu.uade.lensvest.domain.model.SheetMode
import ar.edu.uade.lensvest.domain.usecase.GetBerryStatusUseCase
import ar.edu.uade.lensvest.domain.usecase.GetInvestorLevelUseCase
import ar.edu.uade.lensvest.domain.usecase.GetUserNameUseCase
import ar.edu.uade.lensvest.domain.usecase.GetUserProfileUseCase
import ar.edu.uade.lensvest.domain.usecase.LogoutUseCase
import ar.edu.uade.lensvest.domain.usecase.ObservePlainExplanationsUseCase
import ar.edu.uade.lensvest.domain.usecase.ObserveSheetModeUseCase
import ar.edu.uade.lensvest.domain.usecase.SetPlainExplanationsUseCase
import ar.edu.uade.lensvest.domain.usecase.SetSheetModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** B3 · Perfil. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    getUserNameUseCase: GetUserNameUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getInvestorLevelUseCase: GetInvestorLevelUseCase,
    private val getBerryStatusUseCase: GetBerryStatusUseCase,
    private val setSheetModeUseCase: SetSheetModeUseCase,
    private val setPlainExplanationsUseCase: SetPlainExplanationsUseCase,
    private val logoutUseCase: LogoutUseCase,
    observeSheetModeUseCase: ObserveSheetModeUseCase,
    observePlainExplanationsUseCase: ObservePlainExplanationsUseCase,
) : ViewModel() {

    private val account = MutableStateFlow(
        ProfileAccount(name = getUserNameUseCase().orEmpty(), level = getInvestorLevelUseCase()),
    )

    val uiState = combine(account, observeSheetModeUseCase(), observePlainExplanationsUseCase()) { a, mode, plain ->
        ProfileUiState(a, mode, plain)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState(account.value, SheetMode.SIMPLE, true))

    /** Called when the screen shows: the level or the Berry connection may have changed elsewhere. */
    fun refresh() {
        account.update { it.copy(level = getInvestorLevelUseCase()) }
        viewModelScope.launch {
            try {
                val user = getUserProfileUseCase()
                account.update { it.copy(name = user.name, email = user.email) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The name from the login is enough to show the screen.
            }
        }
        viewModelScope.launch {
            try {
                val connected = getBerryStatusUseCase().connected
                account.update { it.copy(isBerryConnected = connected) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                account.update { it.copy(isBerryConnected = null) }
            }
        }
    }

    fun setMode(mode: SheetMode) = setSheetModeUseCase(mode)

    fun setPlainExplanations(enabled: Boolean) = setPlainExplanationsUseCase(enabled)

    fun logout() = logoutUseCase()
}

data class ProfileAccount(
    val name: String,
    val email: String = "",
    val level: InvestorLevel,
    /** Null while checking, or when the check failed. */
    val isBerryConnected: Boolean? = null,
)

data class ProfileUiState(val account: ProfileAccount, val mode: SheetMode, val plainExplanations: Boolean)

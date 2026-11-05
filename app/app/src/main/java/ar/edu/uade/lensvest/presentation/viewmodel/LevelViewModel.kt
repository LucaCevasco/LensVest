package ar.edu.uade.lensvest.presentation.viewmodel

import androidx.lifecycle.ViewModel
import ar.edu.uade.lensvest.domain.model.InvestorLevel
import ar.edu.uade.lensvest.domain.usecase.GetInvestorLevelUseCase
import ar.edu.uade.lensvest.domain.usecase.SetInvestorLevelUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LevelViewModel @Inject constructor(
    private val setInvestorLevelUseCase: SetInvestorLevelUseCase,
    getInvestorLevelUseCase: GetInvestorLevelUseCase,
) : ViewModel() {

    private val _selected = MutableStateFlow(getInvestorLevelUseCase())
    val selected = _selected.asStateFlow()

    fun select(level: InvestorLevel) {
        _selected.value = level
    }

    fun confirm() = setInvestorLevelUseCase(_selected.value)
}

package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.SheetMode
import ar.edu.uade.lensvest.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ObserveSheetModeUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    operator fun invoke(): StateFlow<SheetMode> = repository.sheetMode
}

class SetSheetModeUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    operator fun invoke(mode: SheetMode) = repository.setSheetMode(mode)
}

class ObservePlainExplanationsUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    operator fun invoke(): StateFlow<Boolean> = repository.plainExplanations
}

class SetPlainExplanationsUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    operator fun invoke(enabled: Boolean) = repository.setPlainExplanations(enabled)
}

class GetInvestorLevelUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    operator fun invoke() = repository.investorLevel()
}

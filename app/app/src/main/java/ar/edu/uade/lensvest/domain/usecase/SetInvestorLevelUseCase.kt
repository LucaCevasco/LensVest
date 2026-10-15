package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.InvestorLevel
import ar.edu.uade.lensvest.domain.repository.ProfileRepository
import javax.inject.Inject

class SetInvestorLevelUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    operator fun invoke(level: InvestorLevel) = repository.setInvestorLevel(level)
}

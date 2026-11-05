package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.Strategy
import ar.edu.uade.lensvest.domain.repository.StrategyRepository
import javax.inject.Inject

class SetStrategyActiveUseCase @Inject constructor(
    private val repository: StrategyRepository,
) {
    suspend operator fun invoke(id: Long, active: Boolean): Strategy = repository.setActive(id, active)
}

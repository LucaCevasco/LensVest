package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.Strategy
import ar.edu.uade.lensvest.domain.repository.StrategyRepository
import javax.inject.Inject

class GetStrategiesUseCase @Inject constructor(
    private val repository: StrategyRepository,
) {
    suspend operator fun invoke(): List<Strategy> = repository.strategies()
}

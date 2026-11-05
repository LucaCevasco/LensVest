package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.repository.StrategyRepository
import javax.inject.Inject

class DeleteStrategyUseCase @Inject constructor(
    private val repository: StrategyRepository,
) {
    suspend operator fun invoke(id: Long) = repository.delete(id)
}

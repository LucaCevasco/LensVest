package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.AssistantTurn
import ar.edu.uade.lensvest.domain.repository.AssistantRepository
import javax.inject.Inject

class ResolvePendingActionsUseCase @Inject constructor(
    private val repository: AssistantRepository,
) {
    suspend operator fun invoke(approve: Boolean): AssistantTurn = repository.resolvePendingActions(approve)
}

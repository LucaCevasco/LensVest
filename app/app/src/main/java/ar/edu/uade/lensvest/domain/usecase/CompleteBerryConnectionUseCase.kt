package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.BerryStatus
import ar.edu.uade.lensvest.domain.repository.BerryRepository
import javax.inject.Inject

class CompleteBerryConnectionUseCase @Inject constructor(
    private val repository: BerryRepository,
) {
    suspend operator fun invoke(code: String, state: String): BerryStatus =
        repository.completeConnection(code, state)
}

package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.repository.BerryRepository
import javax.inject.Inject

class StartBerryConnectionUseCase @Inject constructor(
    private val repository: BerryRepository,
) {
    suspend operator fun invoke(): String = repository.startConnection()
}

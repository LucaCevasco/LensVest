package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.repository.OrderRepository
import javax.inject.Inject

class DiscardOrderUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    suspend operator fun invoke() = repository.discard()
}

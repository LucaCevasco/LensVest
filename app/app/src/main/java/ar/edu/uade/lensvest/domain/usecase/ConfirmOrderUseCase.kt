package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.OrderResult
import ar.edu.uade.lensvest.domain.repository.OrderRepository
import javax.inject.Inject

class ConfirmOrderUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    suspend operator fun invoke(): OrderResult = repository.confirm()
}

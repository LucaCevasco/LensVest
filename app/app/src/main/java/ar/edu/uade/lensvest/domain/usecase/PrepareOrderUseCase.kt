package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.OrderSide
import ar.edu.uade.lensvest.domain.model.PreparedOrder
import ar.edu.uade.lensvest.domain.repository.OrderRepository
import javax.inject.Inject

class PrepareOrderUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    suspend operator fun invoke(ticker: String, side: OrderSide, amountUsd: Double): PreparedOrder {
        require(amountUsd > 0) { "El monto tiene que ser mayor a cero" }
        return repository.prepare(ticker, side, amountUsd)
    }
}

package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.Portfolio
import ar.edu.uade.lensvest.domain.repository.MarketRepository
import javax.inject.Inject

class GetPortfolioUseCase @Inject constructor(
    private val repository: MarketRepository,
) {
    suspend operator fun invoke(): Portfolio = repository.portfolio()
}

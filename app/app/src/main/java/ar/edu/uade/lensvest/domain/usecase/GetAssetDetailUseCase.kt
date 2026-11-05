package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.AssetDetail
import ar.edu.uade.lensvest.domain.repository.MarketRepository
import javax.inject.Inject

class GetAssetDetailUseCase @Inject constructor(
    private val repository: MarketRepository,
) {
    suspend operator fun invoke(ticker: String): AssetDetail = repository.asset(ticker.trim().uppercase())
}

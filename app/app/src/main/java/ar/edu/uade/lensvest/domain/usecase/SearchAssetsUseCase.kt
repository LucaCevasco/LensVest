package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.AssetSearchResult
import ar.edu.uade.lensvest.domain.repository.MarketRepository
import javax.inject.Inject

class SearchAssetsUseCase @Inject constructor(
    private val repository: MarketRepository,
) {
    suspend operator fun invoke(query: String): List<AssetSearchResult> =
        query.trim().takeIf { it.length >= 2 }?.let { repository.search(it) }.orEmpty()
}

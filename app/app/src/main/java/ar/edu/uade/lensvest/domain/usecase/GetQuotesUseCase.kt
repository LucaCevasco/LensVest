package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.Quote
import ar.edu.uade.lensvest.domain.repository.MarketRepository
import javax.inject.Inject

class GetQuotesUseCase @Inject constructor(
    private val repository: MarketRepository,
) {
    suspend operator fun invoke(tickers: List<String>): List<Quote> =
        tickers.map { it.uppercase() }.distinct().takeIf { it.isNotEmpty() }?.let { repository.quotes(it) }.orEmpty()
}

package ar.edu.uade.lensvest.domain.repository

import ar.edu.uade.lensvest.domain.model.AssetDetail
import ar.edu.uade.lensvest.domain.model.AssetSearchResult
import ar.edu.uade.lensvest.domain.model.Portfolio
import ar.edu.uade.lensvest.domain.model.Quote

/** Read-only data from the user's own Berry account. */
interface MarketRepository {
    suspend fun portfolio(): Portfolio
    suspend fun asset(ticker: String): AssetDetail
    suspend fun quotes(tickers: List<String>): List<Quote>
    suspend fun search(query: String): List<AssetSearchResult>
}

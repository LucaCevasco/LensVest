package ar.edu.uade.lensvest.data.repository

import ar.edu.uade.lensvest.data.remote.api.LensVestApi
import ar.edu.uade.lensvest.domain.model.AssetDetail
import ar.edu.uade.lensvest.domain.model.AssetSearchResult
import ar.edu.uade.lensvest.domain.model.Portfolio
import ar.edu.uade.lensvest.domain.model.Quote
import ar.edu.uade.lensvest.domain.repository.MarketRepository
import ar.edu.uade.lensvest.mapper.toDomain
import javax.inject.Inject

class MarketRepositoryImpl @Inject constructor(
    private val api: LensVestApi,
) : MarketRepository {

    override suspend fun portfolio(): Portfolio = api.portfolio().toDomain()

    override suspend fun asset(ticker: String): AssetDetail = api.asset(ticker).toDomain()

    override suspend fun quotes(tickers: List<String>): List<Quote> =
        api.quotes(tickers.joinToString(",")).quotes.map { it.toDomain() }

    override suspend fun search(query: String): List<AssetSearchResult> =
        api.search(query).results.map { it.toDomain() }
}

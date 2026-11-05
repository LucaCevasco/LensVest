package ar.edu.uade.lensvest.mapper

import ar.edu.uade.lensvest.data.remote.dto.AssetResponse
import ar.edu.uade.lensvest.data.remote.dto.CurrentUserResponse
import ar.edu.uade.lensvest.data.remote.dto.PortfolioResponse
import ar.edu.uade.lensvest.data.remote.dto.QuoteDto
import ar.edu.uade.lensvest.data.remote.dto.SearchResultDto
import ar.edu.uade.lensvest.domain.model.AssetDetail
import ar.edu.uade.lensvest.domain.model.AssetSearchResult
import ar.edu.uade.lensvest.domain.model.Holding
import ar.edu.uade.lensvest.domain.model.Portfolio
import ar.edu.uade.lensvest.domain.model.Position
import ar.edu.uade.lensvest.domain.model.ProFigures
import ar.edu.uade.lensvest.domain.model.Quote
import ar.edu.uade.lensvest.domain.model.UserProfile

fun PortfolioResponse.toDomain() = Portfolio(
    totalValueUsd = totalValueUsd,
    cashUsd = cashUsd,
    changeUsd = changeUsd,
    changePct = changePct,
    history = history,
    positions = positions.map { Position(it.ticker, it.name, it.quantity, it.valueUsd, it.changePct) },
)

fun AssetResponse.toDomain() = AssetDetail(
    ticker = ticker,
    name = name,
    priceUsd = priceUsd,
    changePct = changePct,
    history = history,
    about = about,
    brands = brands,
    // A zero quantity is "you don't own it", whatever else the agent filled in.
    position = position?.takeIf { it.quantity > 0 }?.let {
        Holding(it.quantity, it.valueUsd, it.gainUsd, it.gainPct, it.avgPriceUsd)
    },
    cashUsd = cashUsd,
    pro = ProFigures(open, dayHigh, dayLow, previousClose, volume, ceo, founded?.take(4), employees, headquarters),
)

fun QuoteDto.toDomain() = Quote(ticker, name, priceUsd, changePct)

fun SearchResultDto.toDomain() = AssetSearchResult(ticker, name)

fun CurrentUserResponse.toDomain() = UserProfile(name, email)

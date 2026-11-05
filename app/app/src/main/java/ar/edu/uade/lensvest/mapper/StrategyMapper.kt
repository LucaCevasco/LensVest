package ar.edu.uade.lensvest.mapper

import ar.edu.uade.lensvest.data.remote.dto.ScanDto
import ar.edu.uade.lensvest.data.remote.dto.StrategyDto
import ar.edu.uade.lensvest.domain.model.OrderSide
import ar.edu.uade.lensvest.domain.model.Scan
import ar.edu.uade.lensvest.domain.model.Strategy
import ar.edu.uade.lensvest.domain.model.StrategyCondition

fun StrategyDto.toDomain() = Strategy(
    id = id,
    ticker = ticker,
    name = name,
    title = title,
    condition = when (condition) {
        "price_above" -> StrategyCondition.PRICE_ABOVE
        "drop_pct" -> StrategyCondition.DROP_PCT
        "rise_pct" -> StrategyCondition.RISE_PCT
        else -> StrategyCondition.PRICE_BELOW
    },
    threshold = threshold,
    referencePriceUsd = referencePrice,
    side = if (side == "sell") OrderSide.SELL else OrderSide.BUY,
    amountUsd = amountUsd,
    isActive = status == "active",
    updatedAt = updatedAt,
)

fun ScanDto.toDomain() = Scan(id, brand, company, ticker, favorite, createdAt)

package ar.edu.uade.lensvest.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PositionDto(
    val ticker: String,
    val name: String,
    val quantity: Double,
    @SerialName("value_usd") val valueUsd: Double,
    @SerialName("change_pct") val changePct: Double? = null,
)

@Serializable
data class PortfolioResponse(
    @SerialName("total_value_usd") val totalValueUsd: Double,
    @SerialName("cash_usd") val cashUsd: Double? = null,
    @SerialName("change_usd") val changeUsd: Double? = null,
    @SerialName("change_pct") val changePct: Double? = null,
    val history: List<Double> = emptyList(),
    val positions: List<PositionDto> = emptyList(),
)

@Serializable
data class HoldingDto(
    val quantity: Double,
    @SerialName("value_usd") val valueUsd: Double,
    @SerialName("gain_usd") val gainUsd: Double? = null,
    @SerialName("gain_pct") val gainPct: Double? = null,
    @SerialName("avg_price_usd") val avgPriceUsd: Double? = null,
)

@Serializable
data class AssetResponse(
    val ticker: String,
    val name: String,
    @SerialName("price_usd") val priceUsd: Double,
    @SerialName("change_pct") val changePct: Double? = null,
    val history: List<Double> = emptyList(),
    val about: String = "",
    val brands: List<String> = emptyList(),
    val position: HoldingDto? = null,
    @SerialName("cash_usd") val cashUsd: Double? = null,
    val open: Double? = null,
    @SerialName("day_high") val dayHigh: Double? = null,
    @SerialName("day_low") val dayLow: Double? = null,
    @SerialName("previous_close") val previousClose: Double? = null,
    val volume: String? = null,
    val ceo: String? = null,
    val founded: String? = null,
    val employees: String? = null,
    val headquarters: String? = null,
)

@Serializable
data class QuoteDto(
    val ticker: String,
    val name: String,
    @SerialName("price_usd") val priceUsd: Double,
    @SerialName("change_pct") val changePct: Double? = null,
)

@Serializable
data class QuotesResponse(val quotes: List<QuoteDto> = emptyList())

@Serializable
data class SearchResultDto(val ticker: String, val name: String)

@Serializable
data class SearchResponse(val results: List<SearchResultDto> = emptyList())

@Serializable
data class CurrentUserResponse(val name: String, val email: String)

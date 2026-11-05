package ar.edu.uade.lensvest.domain.model

/** One asset in the user's Berry account. */
data class Position(
    val ticker: String,
    val name: String,
    val quantity: Double,
    val valueUsd: Double,
    val changePct: Double?,
)

data class Portfolio(
    val totalValueUsd: Double,
    val cashUsd: Double?,
    /** Last month's change, when Berry reports it. */
    val changeUsd: Double?,
    val changePct: Double?,
    /** Portfolio value over the last month, oldest first; empty when not available. */
    val history: List<Double>,
    val positions: List<Position>,
) {
    val hasInvestments get() = positions.isNotEmpty()
}

/** What the user owns of one asset. */
data class Holding(
    val quantity: Double,
    val valueUsd: Double,
    val gainUsd: Double?,
    val gainPct: Double?,
    val avgPriceUsd: Double?,
)

/** The extra data the "Pro" sheet shows: today's session and the company, as Berry reports them. */
data class ProFigures(
    val open: Double?,
    val dayHigh: Double?,
    val dayLow: Double?,
    val previousClose: Double?,
    val volume: String?,
    val ceo: String?,
    val founded: String?,
    val employees: String?,
    val headquarters: String?,
)

/** 05 · Ficha de la empresa. */
data class AssetDetail(
    val ticker: String,
    val name: String,
    val priceUsd: Double,
    /** Today's change. */
    val changePct: Double?,
    /** Price over the last month, oldest first. */
    val history: List<Double>,
    val about: String,
    val brands: List<String>,
    /** Null when the user doesn't own it. */
    val position: Holding?,
    val cashUsd: Double?,
    val pro: ProFigures,
)

data class Quote(val ticker: String, val name: String, val priceUsd: Double, val changePct: Double?)

data class AssetSearchResult(val ticker: String, val name: String)

data class UserProfile(val name: String, val email: String)

/** How much detail the asset sheet shows. */
enum class SheetMode { SIMPLE, PRO }

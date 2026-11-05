package ar.edu.uade.lensvest.domain.model

enum class OrderSide { BUY, SELL }

enum class StrategyCondition { PRICE_BELOW, PRICE_ABOVE, DROP_PCT, RISE_PCT }

/** A conditional rule the user activated from the assistant (RF04). */
data class Strategy(
    val id: Long,
    val ticker: String,
    val name: String,
    val title: String,
    val condition: StrategyCondition,
    /** A USD price, or a percentage for DROP_PCT / RISE_PCT. */
    val threshold: Double,
    /** The price when it was created: percentages count from here. */
    val referencePriceUsd: Double,
    val side: OrderSide,
    val amountUsd: Double,
    val isActive: Boolean,
    /** "yyyy-MM-dd…" of the last change, e.g. when it was paused. */
    val updatedAt: String,
) {
    /** The price that triggers it. */
    val triggerPriceUsd: Double
        get() = when (condition) {
            StrategyCondition.PRICE_BELOW, StrategyCondition.PRICE_ABOVE -> threshold
            StrategyCondition.DROP_PCT -> referencePriceUsd * (1 - threshold / 100)
            StrategyCondition.RISE_PCT -> referencePriceUsd * (1 + threshold / 100)
        }
}

/** A brand the user confirmed in Lens Mode. */
data class Scan(
    val id: Long,
    val brand: String,
    val company: String?,
    val ticker: String?,
    val favorite: Boolean,
    /** ISO date-time. */
    val createdAt: String,
)

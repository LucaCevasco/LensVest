package ar.edu.uade.lensvest.presentation.ui.assistant

import ar.edu.uade.lensvest.domain.model.OrderSide
import ar.edu.uade.lensvest.domain.model.PendingAction
import ar.edu.uade.lensvest.domain.model.Strategy
import ar.edu.uade.lensvest.domain.model.StrategyCondition

/** How an operation the agent prepared reads on the confirmation card. */
internal data class OperationSummary(
    val symbol: String?,
    val title: String,
    val subtitle: String,
    val rows: List<Pair<String, String>>,
)

private val symbolKeys = listOf("symbol", "ticker", "asset", "instrument")
private val sideKeys = listOf("side", "action", "direction", "type")
private val amountKeys = listOf("amount", "notional", "value", "amount_usd")
private val quantityKeys = listOf("quantity", "qty", "shares")

private fun Map<String, String>.firstOf(keys: List<String>) =
    keys.firstNotNullOfOrNull { key -> entries.firstOrNull { it.key.equals(key, ignoreCase = true) }?.value }

/**
 * Berry's tool schema isn't fixed on our side, so this reads the usual parameter
 * names and shows anything it doesn't recognize as a raw row.
 */
internal fun PendingAction.summary(): OperationSummary {
    val symbol = parameters.firstOf(symbolKeys)
    val side = parameters.firstOf(sideKeys)?.lowercase()
    val amount = parameters.firstOf(amountKeys)
    val quantity = parameters.firstOf(quantityKeys)
    val known = (symbolKeys + sideKeys + amountKeys + quantityKeys).toSet()

    val verb = when {
        tool == "cancel_order" -> "Cancelar orden"
        side == "sell" -> "Vender"
        else -> "Comprar"
    }
    val rows = buildList {
        amount?.let { add("Monto" to it) }
        quantity?.let { add("Cantidad" to it) }
        parameters.filterKeys { it.lowercase() !in known }.forEach { (key, value) -> add(key to value) }
    }
    return OperationSummary(
        symbol = symbol,
        title = listOfNotNull(verb, symbol).joinToString(" "),
        subtitle = "Vía Berry",
        rows = rows,
    )
}

/** The strategy a `propose_strategy` call describes, to preview it before activating (RF04). */
internal fun PendingAction.strategyPreview() = Strategy(
    id = 0,
    ticker = parameters["ticker"].orEmpty(),
    name = parameters["name"].orEmpty(),
    title = parameters["title"].orEmpty(),
    condition = when (parameters["condition"]) {
        "price_above" -> StrategyCondition.PRICE_ABOVE
        "drop_pct" -> StrategyCondition.DROP_PCT
        "rise_pct" -> StrategyCondition.RISE_PCT
        else -> StrategyCondition.PRICE_BELOW
    },
    threshold = parameters["threshold"]?.toDoubleOrNull() ?: 0.0,
    referencePriceUsd = parameters["reference_price"]?.toDoubleOrNull() ?: 0.0,
    side = if (parameters["side"] == "sell") OrderSide.SELL else OrderSide.BUY,
    amountUsd = parameters["amount_usd"]?.toDoubleOrNull() ?: 0.0,
    isActive = false,
    updatedAt = "",
)

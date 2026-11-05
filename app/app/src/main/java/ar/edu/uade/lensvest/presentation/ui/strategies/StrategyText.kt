package ar.edu.uade.lensvest.presentation.ui.strategies

import ar.edu.uade.lensvest.domain.model.OrderSide
import ar.edu.uade.lensvest.domain.model.Strategy
import ar.edu.uade.lensvest.domain.model.StrategyCondition
import ar.edu.uade.lensvest.presentation.ui.components.formatUsd
import java.text.NumberFormat
import java.util.Locale

private fun pct(value: Double) = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-AR"))
    .apply { maximumFractionDigits = 1 }.format(value) + " %"

/** "Si NVDA baja 10 % (US$ 163,79)". */
fun Strategy.conditionText(): String = when (condition) {
    StrategyCondition.PRICE_BELOW -> "Si $ticker baja a ${formatUsd(threshold)}"
    StrategyCondition.PRICE_ABOVE -> "Si $ticker sube a ${formatUsd(threshold)}"
    StrategyCondition.DROP_PCT -> "Si $ticker baja ${pct(threshold)} (${formatUsd(triggerPriceUsd)})"
    StrategyCondition.RISE_PCT -> "Si $ticker sube ${pct(threshold)} (${formatUsd(triggerPriceUsd)})"
}

/** "Comprar US$ 100 de NVDA". */
fun Strategy.actionText(): String =
    "${if (side == OrderSide.SELL) "Vender" else "Comprar"} ${formatUsd(amountUsd)} de $ticker"

/** One line for lists: "US$ 100 · una sola vez". */
fun Strategy.summary(): String = "${formatUsd(amountUsd)} · una sola vez"

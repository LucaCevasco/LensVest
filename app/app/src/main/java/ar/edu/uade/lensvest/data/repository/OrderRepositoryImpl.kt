package ar.edu.uade.lensvest.data.repository

import ar.edu.uade.lensvest.data.remote.api.LensVestApi
import ar.edu.uade.lensvest.data.remote.dto.AssistantConfirmRequest
import ar.edu.uade.lensvest.data.remote.dto.AssistantMessageRequest
import ar.edu.uade.lensvest.domain.model.OrderNotPreparedException
import ar.edu.uade.lensvest.domain.model.OrderResult
import ar.edu.uade.lensvest.domain.model.OrderSide
import ar.edu.uade.lensvest.domain.model.PLACE_ORDER
import ar.edu.uade.lensvest.domain.model.PreparedOrder
import ar.edu.uade.lensvest.mapper.toDomain
import ar.edu.uade.lensvest.domain.repository.OrderRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Orders use the same path as the chat: a prepared prompt to the assistant,
 * which calls Berry's place_order and pauses until the user confirms (RNF01).
 */
class OrderRepositoryImpl @Inject constructor(
    private val api: LensVestApi,
) : OrderRepository {

    override suspend fun prepare(ticker: String, side: OrderSide, amountUsd: Double): PreparedOrder {
        val verb = if (side == OrderSide.SELL) "Vendé" else "Comprá"
        val amount = BigDecimal.valueOf(amountUsd).stripTrailingZeros().toPlainString()
        val turn = api.assistantMessage(AssistantMessageRequest("$verb USD $amount de $ticker con una orden de mercado.")).toDomain()
        val action = turn.pendingActions.firstOrNull { it.tool == PLACE_ORDER }
            ?: throw OrderNotPreparedException(turn.reply.ifBlank { "No se pudo preparar la orden. Probá de nuevo." })
        val p = action.parameters
        return PreparedOrder(
            ticker = p["ticker"] ?: ticker,
            side = if (p["side"] == "sell") OrderSide.SELL else OrderSide.BUY,
            amount = p["amount"]?.toDoubleOrNull() ?: amountUsd,
            amountUnit = p["amount_unit"] ?: "USD",
            orderType = p["order_type"] ?: "market",
        )
    }

    override suspend fun confirm(): OrderResult {
        val turn = api.assistantConfirm(AssistantConfirmRequest(approve = true)).toDomain()
        val placed = turn.executed.firstOrNull { it.tool == PLACE_ORDER }
            ?: return OrderResult(executed = false, orderId = null, message = turn.reply.ifBlank { "Berry no confirmó la orden." })
        return OrderResult(
            executed = placed.ok,
            orderId = if (placed.ok) orderId(placed.result) else null,
            message = if (placed.ok) turn.reply else turn.reply.ifBlank { placed.result },
        )
    }

    override suspend fun discard() {
        api.assistantConfirm(AssistantConfirmRequest(approve = false))
    }

    /** Berry answers with JSON; its order id is under "order_id" or "id", maybe inside "order". */
    private fun orderId(result: String): String? = runCatching {
        val root = Json.parseToJsonElement(result) as JsonObject
        val order = root["order"] as? JsonObject ?: root
        (order["order_id"] ?: order["id"])?.jsonPrimitive?.content
    }.getOrNull()
}

package ar.edu.uade.lensvest.domain.model

/** An order the agent prepared on Berry and that waits for the user (07 · Revisión, RNF01). */
data class PreparedOrder(
    val ticker: String,
    val side: OrderSide,
    val amount: Double,
    /** "USD" or "SHARES", as Berry takes it. */
    val amountUnit: String,
    val orderType: String,
)

/** What Berry answered once the user confirmed. Only `executed` orders show as done (RNF06). */
data class OrderResult(val executed: Boolean, val orderId: String?, val message: String)

/** The assistant answered with text instead of preparing the order, e.g. not enough cash. */
class OrderNotPreparedException(message: String) : Exception(message)

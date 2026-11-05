package ar.edu.uade.lensvest.domain.model

enum class ChatRole { USER, ASSISTANT }

data class ChatMessage(val role: ChatRole, val text: String)

/** An operation the assistant prepared and the user must confirm before it reaches Berry (RNF01). */
data class PendingAction(
    val id: String,
    val tool: String,
    /** Parameters as the agent sent them, e.g. "symbol" to "KO". */
    val parameters: Map<String, String>,
)

/** A Berry tool the user approved and what Berry answered. */
data class ExecutedAction(val tool: String, val parameters: Map<String, String>, val ok: Boolean, val result: String)

/** What the assistant answered, plus anything waiting on the user's confirmation. */
data class AssistantTurn(
    val reply: String,
    val pendingActions: List<PendingAction>,
    val executed: List<ExecutedAction> = emptyList(),
    /** The strategy the user just activated, if that's what they confirmed. */
    val activatedStrategy: Strategy? = null,
)

/** Our own tool: the assistant proposes a strategy and the user activates it (E1). */
const val PROPOSE_STRATEGY = "propose_strategy"
const val PLACE_ORDER = "place_order"

val PendingAction.isStrategyProposal get() = tool == PROPOSE_STRATEGY

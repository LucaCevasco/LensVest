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

/** What the assistant answered, plus anything waiting on the user's confirmation. */
data class AssistantTurn(val reply: String, val pendingActions: List<PendingAction>)

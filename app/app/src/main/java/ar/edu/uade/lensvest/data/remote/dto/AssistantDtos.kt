package ar.edu.uade.lensvest.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AssistantMessageRequest(val text: String)

@Serializable
data class AssistantConfirmRequest(val approve: Boolean)

@Serializable
data class PendingActionDto(val id: String, val tool: String, val input: JsonObject = JsonObject(emptyMap()))

@Serializable
data class AssistantTurnResponse(
    val reply: String,
    @SerialName("pending_actions") val pendingActions: List<PendingActionDto>,
)

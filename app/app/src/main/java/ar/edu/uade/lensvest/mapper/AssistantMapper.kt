package ar.edu.uade.lensvest.mapper

import ar.edu.uade.lensvest.data.remote.dto.AssistantTurnResponse
import ar.edu.uade.lensvest.data.remote.dto.PendingActionDto
import ar.edu.uade.lensvest.domain.model.AssistantTurn
import ar.edu.uade.lensvest.domain.model.PendingAction
import kotlinx.serialization.json.JsonPrimitive

fun AssistantTurnResponse.toDomain() = AssistantTurn(
    reply = reply,
    pendingActions = pendingActions.map { it.toDomain() },
)

fun PendingActionDto.toDomain() = PendingAction(
    id = id,
    tool = tool,
    parameters = input.mapValues { (_, value) ->
        // Strings without quotes; numbers, booleans and nested objects as JSON text.
        (value as? JsonPrimitive)?.takeIf { it.isString }?.content ?: value.toString()
    },
)

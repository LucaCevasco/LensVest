package ar.edu.uade.lensvest.domain.repository

import ar.edu.uade.lensvest.domain.model.AssistantTurn

/** The user's conversation with the LensVest agent, which acts on their own Berry account. */
interface AssistantRepository {
    suspend fun sendMessage(text: String): AssistantTurn

    /** Approves or rejects every operation the assistant is waiting on. */
    suspend fun resolvePendingActions(approve: Boolean): AssistantTurn
}

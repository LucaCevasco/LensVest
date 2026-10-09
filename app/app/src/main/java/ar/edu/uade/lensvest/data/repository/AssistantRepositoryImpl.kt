package ar.edu.uade.lensvest.data.repository

import ar.edu.uade.lensvest.data.remote.api.LensVestApi
import ar.edu.uade.lensvest.data.remote.dto.AssistantConfirmRequest
import ar.edu.uade.lensvest.data.remote.dto.AssistantMessageRequest
import ar.edu.uade.lensvest.domain.model.AssistantTurn
import ar.edu.uade.lensvest.domain.repository.AssistantRepository
import ar.edu.uade.lensvest.mapper.toDomain
import javax.inject.Inject

class AssistantRepositoryImpl @Inject constructor(
    private val api: LensVestApi,
) : AssistantRepository {

    override suspend fun sendMessage(text: String): AssistantTurn =
        api.assistantMessage(AssistantMessageRequest(text)).toDomain()

    override suspend fun resolvePendingActions(approve: Boolean): AssistantTurn =
        api.assistantConfirm(AssistantConfirmRequest(approve)).toDomain()
}

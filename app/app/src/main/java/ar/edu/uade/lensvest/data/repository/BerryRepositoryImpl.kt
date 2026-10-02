package ar.edu.uade.lensvest.data.repository

import ar.edu.uade.lensvest.data.remote.api.LensVestApi
import ar.edu.uade.lensvest.data.remote.dto.BerryCallbackRequest
import ar.edu.uade.lensvest.domain.model.BerryStatus
import ar.edu.uade.lensvest.domain.repository.BerryRepository
import ar.edu.uade.lensvest.mapper.toDomain
import javax.inject.Inject

class BerryRepositoryImpl @Inject constructor(
    private val api: LensVestApi,
) : BerryRepository {

    override suspend fun startConnection(): String = api.berryConnect().authorizeUrl

    override suspend fun completeConnection(code: String, state: String): BerryStatus =
        api.berryCallback(BerryCallbackRequest(code, state)).toDomain()

    override suspend fun getStatus(): BerryStatus = api.berryStatus().toDomain()
}

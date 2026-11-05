package ar.edu.uade.lensvest.data.repository

import ar.edu.uade.lensvest.data.remote.api.LensVestApi
import ar.edu.uade.lensvest.data.remote.dto.StrategyStatusRequest
import ar.edu.uade.lensvest.domain.model.Strategy
import ar.edu.uade.lensvest.domain.repository.StrategyRepository
import ar.edu.uade.lensvest.mapper.toDomain
import javax.inject.Inject

class StrategyRepositoryImpl @Inject constructor(
    private val api: LensVestApi,
) : StrategyRepository {

    override suspend fun strategies(): List<Strategy> = api.strategies().map { it.toDomain() }

    override suspend fun setActive(id: Long, active: Boolean): Strategy =
        api.updateStrategy(id, StrategyStatusRequest(if (active) "active" else "paused")).toDomain()

    override suspend fun delete(id: Long) = api.deleteStrategy(id)
}

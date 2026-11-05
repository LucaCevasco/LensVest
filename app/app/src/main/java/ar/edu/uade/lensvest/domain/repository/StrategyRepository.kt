package ar.edu.uade.lensvest.domain.repository

import ar.edu.uade.lensvest.domain.model.Strategy

interface StrategyRepository {
    suspend fun strategies(): List<Strategy>
    suspend fun setActive(id: Long, active: Boolean): Strategy
    suspend fun delete(id: Long)
}

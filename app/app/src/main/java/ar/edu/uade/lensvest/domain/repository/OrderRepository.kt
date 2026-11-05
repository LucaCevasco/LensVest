package ar.edu.uade.lensvest.domain.repository

import ar.edu.uade.lensvest.domain.model.OrderResult
import ar.edu.uade.lensvest.domain.model.OrderSide
import ar.edu.uade.lensvest.domain.model.PreparedOrder

/** Market orders on Berry. They go through the assistant, which pauses them until the user confirms. */
interface OrderRepository {
    /** Prepares the order; nothing reaches Berry yet. */
    suspend fun prepare(ticker: String, side: OrderSide, amountUsd: Double): PreparedOrder

    /** Sends the prepared order to Berry. */
    suspend fun confirm(): OrderResult

    /** Drops the prepared order without sending it. */
    suspend fun discard()
}

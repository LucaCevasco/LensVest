package ar.edu.uade.lensvest.domain.repository

import ar.edu.uade.lensvest.domain.model.BerryStatus

/** Berry connection. The backend owns the OAuth flow and tokens; the app only relays the redirect. */
interface BerryRepository {
    /** URL of Berry's authorization page, to open in the browser. */
    suspend fun startConnection(): String

    /** Finishes the connection with what Berry sent to lensvest://berry/callback. */
    suspend fun completeConnection(code: String, state: String): BerryStatus

    suspend fun getStatus(): BerryStatus
}

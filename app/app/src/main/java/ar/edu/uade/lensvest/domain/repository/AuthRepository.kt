package ar.edu.uade.lensvest.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Whether there's a saved session; it survives restarts and ends on logout or when it expires. */
    fun isSignedIn(): Boolean
    val signedIn: Flow<Boolean>

    /** Signs in and keeps the session token for the following requests. */
    suspend fun login(email: String, password: String)
    suspend fun register(name: String, email: String, password: String)
    fun logout()
}

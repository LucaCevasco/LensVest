package ar.edu.uade.lensvest.domain.repository

interface AuthRepository {
    /** Signs in and keeps the session token for the following requests. */
    suspend fun login(email: String, password: String)
    suspend fun register(name: String, email: String, password: String)
}

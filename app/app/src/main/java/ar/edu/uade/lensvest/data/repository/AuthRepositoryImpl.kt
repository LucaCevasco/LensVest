package ar.edu.uade.lensvest.data.repository

import ar.edu.uade.lensvest.data.local.SessionStore
import ar.edu.uade.lensvest.data.remote.api.LensVestApi
import ar.edu.uade.lensvest.data.remote.dto.LoginRequest
import ar.edu.uade.lensvest.data.remote.dto.RegisterRequest
import ar.edu.uade.lensvest.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: LensVestApi,
    private val session: SessionStore,
) : AuthRepository {

    override fun isSignedIn(): Boolean = session.token.value != null

    override val signedIn: Flow<Boolean> = session.token.map { it != null }.distinctUntilChanged()

    override suspend fun login(email: String, password: String) {
        val response = api.login(LoginRequest(email, password))
        session.save(response.token, response.name)
    }

    override suspend fun register(name: String, email: String, password: String) =
        api.register(RegisterRequest(email, password, name))

    override fun logout() = session.clear()
}

package ar.edu.uade.lensvest.data.repository

import ar.edu.uade.lensvest.data.local.SessionStore
import ar.edu.uade.lensvest.data.remote.api.LensVestApi
import ar.edu.uade.lensvest.data.remote.dto.LoginRequest
import ar.edu.uade.lensvest.data.remote.dto.RegisterRequest
import ar.edu.uade.lensvest.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: LensVestApi,
    private val session: SessionStore,
) : AuthRepository {

    override suspend fun login(email: String, password: String) {
        session.save(api.login(LoginRequest(email, password)).token)
    }

    override suspend fun register(name: String, email: String, password: String) =
        api.register(RegisterRequest(email, password, name))
}

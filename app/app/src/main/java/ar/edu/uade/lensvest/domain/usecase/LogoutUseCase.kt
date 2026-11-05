package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    operator fun invoke() = repository.logout()
}

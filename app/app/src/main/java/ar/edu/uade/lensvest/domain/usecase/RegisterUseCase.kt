package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.repository.AuthRepository
import javax.inject.Inject

/** Creates the account and signs in right away. */
class RegisterUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(name: String, email: String, password: String) {
        // The backend answers 200 even for a taken email, so login is what tells us it worked.
        repository.register(name, email, password)
        repository.login(email, password)
    }
}

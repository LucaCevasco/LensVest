package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Whether the user is signed in now, and as it changes (logout, expired session). */
class ObserveSessionUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    fun isSignedIn(): Boolean = repository.isSignedIn()

    operator fun invoke(): Flow<Boolean> = repository.signedIn
}

package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.UserProfile
import ar.edu.uade.lensvest.domain.repository.ProfileRepository
import javax.inject.Inject

class GetUserProfileUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(): UserProfile = repository.currentUser()
}

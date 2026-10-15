package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.repository.ProfileRepository
import javax.inject.Inject

class GetUserNameUseCase @Inject constructor(
    private val repository: ProfileRepository,
) {
    operator fun invoke(): String? = repository.userName()
}

package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.RecognizedBrand
import ar.edu.uade.lensvest.domain.repository.LensRepository
import javax.inject.Inject

class RecognizeBrandUseCase @Inject constructor(
    private val repository: LensRepository,
) {
    suspend operator fun invoke(jpeg: ByteArray): RecognizedBrand? = repository.recognizeBrand(jpeg)
}

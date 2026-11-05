package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.Scan
import ar.edu.uade.lensvest.domain.repository.ScanRepository
import javax.inject.Inject

class SetScanFavoriteUseCase @Inject constructor(
    private val repository: ScanRepository,
) {
    suspend operator fun invoke(id: Long, favorite: Boolean): Scan = repository.setFavorite(id, favorite)
}

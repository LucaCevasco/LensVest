package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.Scan
import ar.edu.uade.lensvest.domain.repository.ScanRepository
import javax.inject.Inject

class GetScanHistoryUseCase @Inject constructor(
    private val repository: ScanRepository,
) {
    suspend operator fun invoke(): List<Scan> = repository.history()
}

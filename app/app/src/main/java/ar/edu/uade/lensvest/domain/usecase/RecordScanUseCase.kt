package ar.edu.uade.lensvest.domain.usecase

import ar.edu.uade.lensvest.domain.model.RecognizedBrand
import ar.edu.uade.lensvest.domain.model.Scan
import ar.edu.uade.lensvest.domain.repository.ScanRepository
import javax.inject.Inject

class RecordScanUseCase @Inject constructor(
    private val repository: ScanRepository,
) {
    suspend operator fun invoke(brand: RecognizedBrand): Scan = repository.record(brand)
}

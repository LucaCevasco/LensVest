package ar.edu.uade.lensvest.data.repository

import ar.edu.uade.lensvest.data.remote.api.LensVestApi
import ar.edu.uade.lensvest.data.remote.dto.FavoriteRequest
import ar.edu.uade.lensvest.data.remote.dto.NewScanRequest
import ar.edu.uade.lensvest.domain.model.RecognizedBrand
import ar.edu.uade.lensvest.domain.model.Scan
import ar.edu.uade.lensvest.domain.repository.ScanRepository
import ar.edu.uade.lensvest.mapper.toDomain
import javax.inject.Inject

class ScanRepositoryImpl @Inject constructor(
    private val api: LensVestApi,
) : ScanRepository {

    override suspend fun history(): List<Scan> = api.scans().map { it.toDomain() }

    override suspend fun record(brand: RecognizedBrand): Scan =
        api.recordScan(NewScanRequest(brand.brand, brand.company, brand.ticker)).toDomain()

    override suspend fun setFavorite(id: Long, favorite: Boolean): Scan =
        api.setScanFavorite(id, FavoriteRequest(favorite)).toDomain()
}

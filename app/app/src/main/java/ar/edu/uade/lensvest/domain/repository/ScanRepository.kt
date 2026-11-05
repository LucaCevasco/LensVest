package ar.edu.uade.lensvest.domain.repository

import ar.edu.uade.lensvest.domain.model.RecognizedBrand
import ar.edu.uade.lensvest.domain.model.Scan

/** Lens Mode history (09 · Tus escaneos). */
interface ScanRepository {
    suspend fun history(): List<Scan>
    suspend fun record(brand: RecognizedBrand): Scan
    suspend fun setFavorite(id: Long, favorite: Boolean): Scan
}

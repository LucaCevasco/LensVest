package ar.edu.uade.lensvest.domain.repository

import ar.edu.uade.lensvest.domain.model.RecognizedBrand

interface LensRepository {
    /** The brand in a JPEG photo, or null when none could be identified. */
    suspend fun recognizeBrand(jpeg: ByteArray): RecognizedBrand?
}

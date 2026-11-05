package ar.edu.uade.lensvest.data.repository

import android.util.Base64
import ar.edu.uade.lensvest.data.remote.api.LensVestApi
import ar.edu.uade.lensvest.data.remote.dto.RecognizeBrandRequest
import ar.edu.uade.lensvest.domain.model.RecognizedBrand
import ar.edu.uade.lensvest.domain.repository.LensRepository
import ar.edu.uade.lensvest.mapper.toDomain
import javax.inject.Inject

class LensRepositoryImpl @Inject constructor(
    private val api: LensVestApi,
) : LensRepository {

    // The photo is only sent for this request; neither the app nor the backend stores it (RNF05).
    override suspend fun recognizeBrand(jpeg: ByteArray): RecognizedBrand? =
        api.recognizeBrand(
            RecognizeBrandRequest(
                imageBase64 = Base64.encodeToString(jpeg, Base64.NO_WRAP),
                mediaType = "image/jpeg",
            ),
        ).toDomain()
}

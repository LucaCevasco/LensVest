package ar.edu.uade.lensvest.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecognizeBrandRequest(
    @SerialName("image_base64") val imageBase64: String,
    @SerialName("media_type") val mediaType: String,
)

@Serializable
data class RecognizedBrandResponse(
    val found: Boolean,
    val brand: String? = null,
    val company: String? = null,
    val ticker: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false,
    val confidence: String = "low",
    val explanation: String = "",
)

package ar.edu.uade.lensvest.mapper

import ar.edu.uade.lensvest.data.remote.dto.RecognizedBrandResponse
import ar.edu.uade.lensvest.domain.model.RecognizedBrand

/** Null when nothing was recognized: better no suggestion than a wrong company. */
fun RecognizedBrandResponse.toDomain(): RecognizedBrand? {
    val brand = brand?.takeIf { found && it.isNotBlank() } ?: return null
    return RecognizedBrand(
        brand = brand,
        company = company,
        ticker = ticker?.takeIf { isPublic && it.isNotBlank() },
        confidence = when (confidence) {
            "high" -> RecognizedBrand.Confidence.HIGH
            "medium" -> RecognizedBrand.Confidence.MEDIUM
            else -> RecognizedBrand.Confidence.LOW
        },
    )
}

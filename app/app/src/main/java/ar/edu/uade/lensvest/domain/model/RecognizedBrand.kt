package ar.edu.uade.lensvest.domain.model

/** What the camera photo shows, and the listed company behind it (if any). */
data class RecognizedBrand(
    val brand: String,
    val company: String?,
    /** US ticker, e.g. "KO"; null when the company isn't public. */
    val ticker: String?,
    val confidence: Confidence,
) {
    enum class Confidence { HIGH, MEDIUM, LOW }

    val isInvestable: Boolean get() = ticker != null
}

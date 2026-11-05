package ar.edu.uade.lensvest.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StrategyDto(
    val id: Long,
    val ticker: String,
    val name: String,
    val title: String,
    val condition: String,
    val threshold: Double,
    @SerialName("reference_price") val referencePrice: Double,
    val side: String,
    @SerialName("amount_usd") val amountUsd: Double,
    val status: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class StrategyStatusRequest(val status: String)

@Serializable
data class ScanDto(
    val id: Long,
    val brand: String,
    val company: String? = null,
    val ticker: String? = null,
    val favorite: Boolean,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class NewScanRequest(val brand: String, val company: String?, val ticker: String?)

@Serializable
data class FavoriteRequest(val favorite: Boolean)

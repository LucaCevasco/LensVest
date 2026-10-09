package ar.edu.uade.lensvest.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BerryConnectResponse(@SerialName("authorize_url") val authorizeUrl: String)

@Serializable
data class BerryCallbackRequest(val code: String, val state: String)

@Serializable
data class BerryStatusResponse(val connected: Boolean)

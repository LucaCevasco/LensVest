package ar.edu.uade.lensvest.mapper

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

@Serializable
private data class ErrorBody(val error: String? = null, val description: String? = null)

private val json = Json { ignoreUnknownKeys = true }

/** A message the user can read, from a backend error body or a network failure. */
fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> response()?.errorBody()?.string()
        ?.let { runCatching { json.decodeFromString<ErrorBody>(it) }.getOrNull() }
        ?.let { it.description ?: it.error }
        ?: "Error del servidor (${code()})"
    is IOException -> "No se pudo conectar con el servidor. Revisá tu conexión y reintentá."
    else -> message ?: "Error inesperado"
}

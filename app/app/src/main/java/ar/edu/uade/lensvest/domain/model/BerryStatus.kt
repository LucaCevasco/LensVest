package ar.edu.uade.lensvest.domain.model

/** Whether the user's Berry account is connected, and the MCP tools it exposes. */
data class BerryStatus(val connected: Boolean, val tools: List<String>)

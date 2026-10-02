package ar.edu.uade.lensvest.mapper

import ar.edu.uade.lensvest.data.remote.dto.BerryStatusResponse
import ar.edu.uade.lensvest.domain.model.BerryStatus

fun BerryStatusResponse.toDomain() = BerryStatus(
    connected = connected,
    tools = tools,
)

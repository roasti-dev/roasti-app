package dev.roasti.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PageResponseDto<T>(
    @SerialName("items") val items: List<T>,
    @SerialName("pagination") val pagination: PaginationResponseDto,
)

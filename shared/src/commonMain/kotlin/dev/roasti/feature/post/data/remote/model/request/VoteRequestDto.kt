package dev.roasti.feature.post.data.remote.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import dev.roasti.feature.post.data.remote.model.VoteDirectionDto

@Serializable
data class VoteRequestDto(
    @SerialName("type")
    val type: VoteDirectionDto,
)

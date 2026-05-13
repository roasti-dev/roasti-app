package dev.roasti.feature.post.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class VoteDirectionDto {
    @SerialName("up") UP,
    @SerialName("down") DOWN,
    @SerialName("none") NONE,
}

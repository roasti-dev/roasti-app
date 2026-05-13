package dev.roasti.feature.post.data.remote.model.response

import dev.roasti.feature.post.data.remote.model.VoteDirectionDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostVoteResponseDto(
    @SerialName("rating")
    val rating: Int,
    @SerialName("user_vote")
    val userVote: VoteDirectionDto = VoteDirectionDto.NONE,
)

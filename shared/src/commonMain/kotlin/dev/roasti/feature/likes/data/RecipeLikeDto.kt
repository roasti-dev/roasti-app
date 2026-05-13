package dev.roasti.feature.likes.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RecipeLikeDto(
    @SerialName("liked")
    val isLiked: Boolean,
    @SerialName("likes_count")
    val likesCount: Int,
)
package dev.roasti.feature.comment.data.remote.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentAuthorDto(
    @SerialName("id")
    val id: String,
    @SerialName("username")
    val username: String,
    @SerialName("name")
    val name: String? = null,
    @SerialName("avatar_id")
    val avatarId: String? = null,
)

package dev.roasti.feature.post.data.remote.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PostAuthorDto(
    @SerialName("id")
    val id: String,
    @SerialName("username")
    val username: String,
    @SerialName("name")
    val name: String? = null,
    @SerialName("avatar_id")
    val avatarId: String? = null,
)

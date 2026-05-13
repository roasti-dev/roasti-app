package dev.roasti.feature.auth.data.network.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("avatar_id")
    val avatarId: String? = null,
    @SerialName("bio")
    val bio: String? = null,
    @SerialName("id")
    val id: String,
    @SerialName("name")
    val name: String? = null,
    @SerialName("username")
    val username: String,
    @SerialName("email")
    val email: String,
)

package dev.roasti.feature.auth.data.network.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Model for public profiles of users
 * Similar to UserDto, but without email
 * @see UserDto
 */
@Serializable
data class PublicUserDto(
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
)

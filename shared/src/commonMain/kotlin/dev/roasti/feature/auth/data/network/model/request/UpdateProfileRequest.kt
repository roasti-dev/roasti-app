package dev.roasti.feature.auth.data.network.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class UpdateProfileRequest(
    @SerialName("avatar_id")
    val imageId: String? = null,
    @SerialName("bio")
    val bio: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("username")
    val username: String? = null,
) {
    fun isEmpty() = imageId == null && bio == null && name == null && username == null
}
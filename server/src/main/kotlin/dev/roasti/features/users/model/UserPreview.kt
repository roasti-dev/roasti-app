package dev.roasti.features.users.model

import dev.roasti.features.uploads.ImageId

data class UserPreview(
    val id: UserId,
    val username: String,
    val name: String?,
    val avatarId: ImageId?,
)

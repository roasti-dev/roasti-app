package dev.roasti.features.users.model

data class UserPreview(
    val id: UserId,
    val username: String,
    val name: String?,
    val avatarId: String?,
)

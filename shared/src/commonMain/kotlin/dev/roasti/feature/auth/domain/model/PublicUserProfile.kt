package dev.roasti.feature.auth.domain.model

import kotlinx.serialization.SerialName

data class PublicUserProfile(
    val avatarId: String? = null,
    val bio: String? = null,
    val id: String,
    val name: String? = null,
    val username: String,
)

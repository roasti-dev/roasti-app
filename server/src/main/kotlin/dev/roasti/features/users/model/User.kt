package dev.roasti.features.users.model

import dev.roasti.features.uploads.ImageId
import kotlin.time.Instant

data class User(
    val id: UserId,
    val firebaseId: FirebaseId,
    val email: Email,
    val username: Username,
    val name: String?,
    val avatarId: ImageId?,
    val bio: String?,
    val createdAt: Instant,
)

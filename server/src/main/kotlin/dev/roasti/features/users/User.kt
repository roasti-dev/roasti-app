package dev.roasti.features.users

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@JvmInline
@OptIn(ExperimentalUuidApi::class)
value class UserId(val value: Uuid)

@JvmInline
value class FirebaseId(val value: String)

data class User(
    val id: UserId,
    val firebaseId: FirebaseId,
    val email: String,
    val username: String,
    val name: String?,
    val avatarId: String?,
    val bio: String?,
    val createdAt: Instant,
)

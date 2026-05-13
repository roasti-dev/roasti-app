package dev.roasti.features.users

import org.jetbrains.exposed.v1.core.Alias
import org.jetbrains.exposed.v1.core.ResultRow

data class UserPreview(
    val id: UserId,
    val username: String,
    val name: String?,
    val avatarId: String?,
)

fun ResultRow.toUserPreview() = UserPreview(
    id = UserId(this[UserTable.id].value),
    username = this[UserTable.username],
    name = this[UserTable.name],
    avatarId = this[UserTable.avatarId],
)

fun ResultRow.toUserPreview(alias: Alias<UserTable>) = UserPreview(
    id = UserId(this[alias[UserTable.id]].value),
    username = this[alias[UserTable.username]],
    name = this[alias[UserTable.name]],
    avatarId = this[alias[UserTable.avatarId]],
)

package dev.roasti.features.users

import dev.roasti.features.users.model.Email
import dev.roasti.features.users.model.FirebaseId
import dev.roasti.features.users.model.User
import dev.roasti.features.users.model.UserId
import dev.roasti.features.users.model.UserPreview
import dev.roasti.features.users.model.Username
import org.jetbrains.exposed.v1.core.Alias
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

object UserTable : UuidTable("users") {
    val firebaseId = varchar("firebase_id", 128)
    val email = varchar("email", 255).uniqueIndex()
    val username = varchar("username", 255).uniqueIndex()
    val name = varchar("name", 255).nullable()
    val avatarId = varchar("avatar_id", 255).nullable()
    val bio = text("bio").nullable()
    val createdAt = timestamp("created_at")
}

@OptIn(ExperimentalUuidApi::class)
fun ResultRow.toUser() = User(
    id = UserId(this[UserTable.id].value),
    firebaseId = FirebaseId(this[UserTable.firebaseId]),
    email = Email.fromDb(this[UserTable.email]),
    username = Username.fromDb(this[UserTable.username]),
    name = this[UserTable.name],
    avatarId = this[UserTable.avatarId],
    bio = this[UserTable.bio],
    createdAt = this[UserTable.createdAt],
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
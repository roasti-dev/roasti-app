package dev.roasti.features.users

import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.UuidTable
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
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
    email = this[UserTable.email],
    username = this[UserTable.username],
    name = this[UserTable.name],
    avatarId = this[UserTable.avatarId],
    bio = this[UserTable.bio],
    createdAt = this[UserTable.createdAt],
)

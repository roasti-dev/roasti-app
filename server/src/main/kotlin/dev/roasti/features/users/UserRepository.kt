package dev.roasti.features.users

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi

interface UserRepository {
    suspend fun findById(id: UserId): User?
    suspend fun findByUsername(username: String): User?
    suspend fun create(input: CreateUserInput): User
    suspend fun update(id: UserId, fields: UpdateUserFields)
    suspend fun existsByUsername(username: String): Boolean
    suspend fun existsByEmail(email: String): Boolean
}

@OptIn(ExperimentalUuidApi::class)
class UserRepositoryImpl : UserRepository {

    override suspend fun findById(id: UserId): User? = withContext(Dispatchers.IO) {
        transaction {
            UserTable.selectAll()
                .where { UserTable.id eq id.value }
                .singleOrNull()
                ?.toUser()
        }
    }

    override suspend fun findByUsername(username: String): User? = withContext(Dispatchers.IO) {
        transaction {
            UserTable.selectAll()
                .where { UserTable.username eq username }
                .singleOrNull()
                ?.toUser()
        }
    }

    override suspend fun create(input: CreateUserInput): User = withContext(Dispatchers.IO) {
        transaction {
            UserTable.insert {
                it[id] = input.id.value
                it[firebaseId] = input.firebaseId.value
                it[email] = input.email
                it[username] = input.username
                it[name] = input.name
                it[avatarId] = input.avatarId
                it[bio] = input.bio
                it[createdAt] = Clock.System.now()
            }
            UserTable.selectAll()
                .where { UserTable.id eq input.id.value }
                .single()
                .toUser()
        }
    }

    override suspend fun update(id: UserId, fields: UpdateUserFields): Unit = withContext(Dispatchers.IO) {
        transaction {
            UserTable.update({ UserTable.id eq id.value }) { stmt ->
                fields.username?.let { stmt[UserTable.username] = it }
                fields.name?.let { stmt[UserTable.name] = it }
                fields.bio?.let { stmt[UserTable.bio] = it }
                fields.avatarId?.let { stmt[UserTable.avatarId] = it }
            }
        }
    }

    override suspend fun existsByUsername(username: String): Boolean = withContext(Dispatchers.IO) {
        transaction {
            UserTable.selectAll()
                .where { UserTable.username eq username }
                .count() > 0
        }
    }

    override suspend fun existsByEmail(email: String): Boolean = withContext(Dispatchers.IO) {
        transaction {
            UserTable.selectAll()
                .where { UserTable.email eq email }
                .count() > 0
        }
    }
}

data class CreateUserInput(
    val id: UserId,
    val firebaseId: FirebaseId,
    val email: String,
    val username: String,
    val name: String? = null,
    val avatarId: String? = null,
    val bio: String? = null,
)

data class UpdateUserFields(
    val username: String? = null,
    val name: String? = null,
    val bio: String? = null,
    val avatarId: String? = null,
)

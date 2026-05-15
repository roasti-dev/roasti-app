package dev.roasti.features.users

import arrow.core.Either
import dev.roasti.features.uploads.ImageId
import dev.roasti.features.users.model.Email
import dev.roasti.features.users.model.User
import dev.roasti.features.users.model.UserId
import dev.roasti.features.users.model.Username
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

sealed interface UpdateUserError {
  data object NotFound : UpdateUserError

  data object UsernameConflict : UpdateUserError
}

interface UserRepository {
  suspend fun findById(id: UserId): User?

  suspend fun findByUsername(username: Username): User?

  suspend fun create(input: User): User

  suspend fun update(id: UserId, fields: UpdateUserFields): Either<UpdateUserError, User>

  suspend fun existsByUsername(username: Username): Boolean

  suspend fun existsByEmail(email: Email): Boolean
}

@OptIn(ExperimentalUuidApi::class)
class UserRepositoryImpl : UserRepository {

  override suspend fun findById(id: UserId): User? =
      withContext(Dispatchers.IO) {
        transaction {
          UserTable.selectAll().where { UserTable.id eq id.value }.singleOrNull()?.toUser()
        }
      }

  override suspend fun findByUsername(username: Username): User? =
      withContext(Dispatchers.IO) {
        transaction {
          UserTable.selectAll()
              .where { UserTable.username eq username.value }
              .singleOrNull()
              ?.toUser()
        }
      }

  override suspend fun create(input: User): User =
      withContext(Dispatchers.IO) {
        // insert + select instead of insertReturning: H2 (used in tests) doesn't support
        // RETURNING
        // clause
        transaction {
          UserTable.insert {
            it[id] = input.id.value
            it[firebaseId] = input.firebaseId.value
            it[email] = input.email.value
            it[username] = input.username.value
            it[name] = input.name
            it[avatarId] = input.avatarId?.value
            it[bio] = input.bio
            it[UserTable.createdAt] = input.createdAt
          }
          UserTable.selectAll().where { UserTable.id eq input.id.value }.single().toUser()
        }
      }

  override suspend fun update(
      id: UserId,
      fields: UpdateUserFields,
  ): Either<UpdateUserError, User> =
      withContext(Dispatchers.IO) {
        // update + select instead of updateReturning: H2 (used in tests) doesn't support
        // RETURNING
        // clause
        try {
          transaction {
            val updated =
                UserTable.update({ UserTable.id eq id.value }) { stmt ->
                  fields.username?.let { stmt[UserTable.username] = it.value }
                  fields.name?.let { stmt[UserTable.name] = it }
                  fields.bio?.let { stmt[UserTable.bio] = it }
                  fields.avatarId?.let { stmt[UserTable.avatarId] = it.value }
                }
            if (updated == 0) return@transaction Either.Left(UpdateUserError.NotFound)
            Either.Right(UserTable.selectAll().where { UserTable.id eq id.value }.single().toUser())
          }
        } catch (e: ExposedSQLException) {
          // 23505 is the PostgreSQL SQLState code for unique_violation
          if (e.sqlState == "23505") Either.Left(UpdateUserError.UsernameConflict) else throw e
        }
      }

  override suspend fun existsByUsername(username: Username): Boolean =
      withContext(Dispatchers.IO) {
        transaction {
          UserTable.selectAll().where { UserTable.username eq username.value }.count() > 0
        }
      }

  override suspend fun existsByEmail(email: Email): Boolean =
      withContext(Dispatchers.IO) {
        transaction { UserTable.selectAll().where { UserTable.email eq email.value }.count() > 0 }
      }
}

data class UpdateUserFields(
    val username: Username? = null,
    val name: String? = null,
    val bio: String? = null,
    val avatarId: ImageId? = null,
)

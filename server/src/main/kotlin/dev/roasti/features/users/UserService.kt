package dev.roasti.features.users

import arrow.core.Either
import arrow.core.left
import arrow.core.right

private val usernameRegex = Regex("^[a-zA-Z0-9_]+$")
private const val USERNAME_MIN_LENGTH = 6
private const val USERNAME_MAX_LENGTH = 16

interface UserService {
    suspend fun getById(id: UserId): Either<UserErrorCode, User>
    suspend fun getByUsername(username: String): Either<UserErrorCode, User>
    suspend fun updateProfile(id: UserId, fields: UpdateUserFields): Either<UserErrorCode, User>
    suspend fun checkUsernameAvailability(username: String): Boolean
    fun validateUsername(username: String): UserErrorCode?
}

class UserServiceImpl(private val repo: UserRepository) : UserService {

    override suspend fun getById(id: UserId): Either<UserErrorCode, User> =
        repo.findById(id)?.right() ?: UserErrorCode.NOT_FOUND.left()

    override suspend fun getByUsername(username: String): Either<UserErrorCode, User> =
        repo.findByUsername(username)?.right() ?: UserErrorCode.NOT_FOUND.left()

    override suspend fun updateProfile(id: UserId, fields: UpdateUserFields): Either<UserErrorCode, User> {
        if (fields.username != null) {
            validateUsername(fields.username)?.let { return it.left() }
            if (repo.existsByUsername(fields.username)) return UserErrorCode.USERNAME_TAKEN.left()
        }
        repo.update(id, fields)
        return getById(id)
    }

    override suspend fun checkUsernameAvailability(username: String): Boolean =
        !repo.existsByUsername(username)

    override fun validateUsername(username: String): UserErrorCode? = when {
        username.length < USERNAME_MIN_LENGTH -> UserErrorCode.USERNAME_TOO_SHORT
        username.length > USERNAME_MAX_LENGTH -> UserErrorCode.USERNAME_TOO_LONG
        !usernameRegex.matches(username) -> UserErrorCode.USERNAME_INVALID_CHARS
        else -> null
    }
}

package dev.roasti.features.users

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.context.bind
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right

private val usernameRegex = Regex("^[a-zA-Z0-9_]+$")
private const val USERNAME_MIN_LENGTH = 6
private const val USERNAME_MAX_LENGTH = 16

sealed interface GetUserError {
    data object NotFound : GetUserError
}

sealed interface UpdateProfileError {
    data object NotFound : UpdateProfileError
    data object UsernameTaken : UpdateProfileError
    data class InvalidUsername(val error: UsernameValidationError) : UpdateProfileError
}

data class UsernameValidationError(val message: String)

interface UserService {
    suspend fun getById(id: UserId): Either<GetUserError, User>
    suspend fun getByUsername(username: String): Either<GetUserError, User>
    suspend fun updateProfile(
        id: UserId,
        fields: UpdateUserFields
    ): Either<UpdateProfileError, User>

    suspend fun checkUsernameAvailability(username: String): Boolean
    fun validateUsername(username: String): Either<UsernameValidationError, Unit>
}

class UserServiceImpl(private val repo: UserRepository) : UserService {

    override suspend fun getById(id: UserId): Either<GetUserError, User> = either {
        repo.findById(id) ?: raise(GetUserError.NotFound)
    }

    override suspend fun getByUsername(username: String): Either<GetUserError, User> = either {
        repo.findByUsername(username) ?: raise(GetUserError.NotFound)
    }

    override suspend fun updateProfile(
        id: UserId,
        fields: UpdateUserFields
    ): Either<UpdateProfileError, User> = either {
        fields.username?.let { username ->
            validateUsername(username)
                .mapLeft { UpdateProfileError.InvalidUsername(it) }
                .bind()

            val currentUser = getById(id).mapLeft { it.toUpdateProfileError() }.bind()

            if (username != currentUser.username) {
                ensure(!repo.existsByUsername(username)) {
                    UpdateProfileError.UsernameTaken
                }
            }
        }

        // TODO: make the update atomic:
        // - use UPDATE ... RETURNING to return an updated user
        // - define a busy username by unique constraint violation instead of the previous existsByUsername
        repo.update(id, fields)

        getById(id).mapLeft { it.toUpdateProfileError() }.bind()
    }

    private fun GetUserError.toUpdateProfileError(): UpdateProfileError =
        when (this) {
            GetUserError.NotFound -> UpdateProfileError.NotFound
        }

    override suspend fun checkUsernameAvailability(username: String): Boolean =
        !repo.existsByUsername(username)

    override fun validateUsername(username: String): Either<UsernameValidationError, Unit> =
        either {
            ensure(username.length >= USERNAME_MIN_LENGTH) { UsernameValidationError("username too short") }
            ensure(username.length <= USERNAME_MAX_LENGTH) { UsernameValidationError("username too long") }
            ensure(usernameRegex.matches(username)) { UsernameValidationError("username contains invalid chars") }
        }
}

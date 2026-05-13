package dev.roasti.features.users.usecase

import arrow.core.Either
import arrow.core.raise.either
import dev.roasti.features.users.UpdateUserError
import dev.roasti.features.users.UpdateUserFields
import dev.roasti.features.users.UserRepository
import dev.roasti.features.users.model.User
import dev.roasti.features.users.model.UserId
import dev.roasti.features.users.model.Username
import dev.roasti.features.users.model.UsernameError

sealed interface UpdateProfileError {
  data object NotFound : UpdateProfileError

  data object UsernameTaken : UpdateProfileError

  data class InvalidUsername(val error: UsernameError) : UpdateProfileError
}

data class UpdateProfileInput(
    val username: String? = null,
    val name: String? = null,
    val bio: String? = null,
    val avatarId: String? = null,
)

class UpdateProfile(private val repo: UserRepository) {
  suspend operator fun invoke(
      id: UserId,
      input: UpdateProfileInput,
  ): Either<UpdateProfileError, User> = either {
    val username =
        input.username?.let {
          Username.create(it).mapLeft { e -> UpdateProfileError.InvalidUsername(e) }.bind()
        }

    repo
        .update(id, UpdateUserFields(username, input.name, input.bio, input.avatarId))
        .mapLeft { it.toUpdateProfileError() }
        .bind()
  }
}

private fun UpdateUserError.toUpdateProfileError() =
    when (this) {
      UpdateUserError.NotFound -> UpdateProfileError.NotFound
      UpdateUserError.UsernameConflict -> UpdateProfileError.UsernameTaken
    }

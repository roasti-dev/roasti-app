package dev.roasti.features.users.usecase

import arrow.core.Either
import arrow.core.raise.either
import dev.roasti.features.users.UserRepository
import dev.roasti.features.users.model.User
import dev.roasti.features.users.model.Username

class GetUserProfile(private val repo: UserRepository) {
  suspend operator fun invoke(username: String): Either<GetUserError, User> = either {
    val parsed = Username.create(username).mapLeft { GetUserError.NotFound }.bind()
    repo.findByUsername(parsed) ?: raise(GetUserError.NotFound)
  }
}

package dev.roasti.features.users.usecase

import arrow.core.Either
import arrow.core.raise.either
import dev.roasti.features.users.model.User
import dev.roasti.features.users.model.UserId
import dev.roasti.features.users.UserRepository

sealed interface GetUserError {
    data object NotFound : GetUserError
}

class GetCurrentUser(private val repo: UserRepository) {
    suspend operator fun invoke(id: UserId): Either<GetUserError, User> = either {
        repo.findById(id) ?: raise(GetUserError.NotFound)
    }
}

package dev.roasti.features.users.usecase

import dev.roasti.features.users.UserRepository
import dev.roasti.features.users.model.Username

class CheckUsernameAvailability(private val repo: UserRepository) {
  suspend operator fun invoke(username: String): Boolean =
      Username.create(username).fold({ false }, { !repo.existsByUsername(it) })
}

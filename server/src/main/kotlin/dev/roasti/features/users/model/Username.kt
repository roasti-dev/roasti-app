package dev.roasti.features.users.model

import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import dev.roasti.common.ValidationResult
import dev.roasti.common.api.FieldError

@JvmInline
value class Username private constructor(val value: String) {
  companion object {
    private const val USERNAME_MIN_LENGTH = 6
    private const val USERNAME_MAX_LENGTH = 16
    private val usernameRegex = Regex("^[a-zA-Z0-9_]+$")

    fun create(username: String): ValidationResult<Username> =
        either {
              zipOrAccumulate(
                  {
                    ensure(username.length in USERNAME_MIN_LENGTH..USERNAME_MAX_LENGTH) {
                      "username must be $USERNAME_MIN_LENGTH-$USERNAME_MAX_LENGTH characters"
                    }
                  },
                  { ensure(usernameRegex.matches(username)) { "username contains invalid chars" } },
              ) { _, _ ->
                Username(username)
              }
            }
            .mapLeft { it.map { msg -> FieldError("username", msg) } }

    fun fromDb(trustedRaw: String): Username = Username(trustedRaw)
  }
}

package dev.roasti.features.users.model

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure

data class UsernameError(val message: String)

@JvmInline
value class Username private constructor(val value: String) {
  companion object {
    private const val USERNAME_MIN_LENGTH = 6
    private const val USERNAME_MAX_LENGTH = 16
    private val usernameRegex = Regex("^[a-zA-Z0-9_]+$")

    fun create(username: String): Either<UsernameError, Username> = either {
      ensure(username.length >= USERNAME_MIN_LENGTH) { UsernameError("username too short") }
      ensure(username.length <= USERNAME_MAX_LENGTH) { UsernameError("username too long") }
      ensure(usernameRegex.matches(username)) { UsernameError("username contains invalid chars") }
      Username(username)
    }

    fun fromDb(trustedRaw: String): Username = Username(trustedRaw)
  }
}

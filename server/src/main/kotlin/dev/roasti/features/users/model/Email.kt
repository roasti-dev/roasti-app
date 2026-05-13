package dev.roasti.features.users.model

import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.roasti.common.ValidationResult
import dev.roasti.common.api.FieldError

@JvmInline
value class Email private constructor(val value: String) {
  companion object {
    private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun create(email: String): ValidationResult<Email> =
        either {
              ensure(emailRegex.matches(email)) { "invalid email format" }
              Email(email)
            }
            .mapLeft { nonEmptyListOf(FieldError("email", it)) }

    fun fromDb(trustedRaw: String): Email = Email(trustedRaw)
  }
}

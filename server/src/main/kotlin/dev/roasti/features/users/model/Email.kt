package dev.roasti.features.users.model

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure

data class EmailError(val message: String)

@JvmInline
value class Email private constructor(val value: String) {
    companion object {
        private val emailRegex = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        fun create(email: String): Either<EmailError, Email> = either {
            ensure(emailRegex.matches(email)) { EmailError("invalid email format") }
            Email(email)
        }

        fun fromDb(trustedRaw: String): Email = Email(trustedRaw)
    }
}

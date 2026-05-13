package dev.roasti.features.users

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import dev.roasti.common.api.ApiError

enum class UserErrorCode { NOT_FOUND, USERNAME_TAKEN, USERNAME_TOO_SHORT, USERNAME_TOO_LONG, USERNAME_INVALID_CHARS }

@Serializable
data class UserError(
    @SerialName("code") override val code: UserErrorCode,
    @SerialName("message") override val message: String,
) : ApiError

fun UserErrorCode.toError() = when (this) {
    UserErrorCode.NOT_FOUND -> UserError(this, "User not found")
    UserErrorCode.USERNAME_TAKEN -> UserError(this, "Username already taken")
    UserErrorCode.USERNAME_TOO_SHORT -> UserError(this, "Username must be at least 6 characters")
    UserErrorCode.USERNAME_TOO_LONG -> UserError(this, "Username must be at most 16 characters")
    UserErrorCode.USERNAME_INVALID_CHARS -> UserError(this, "Username can only contain letters, numbers and underscores")
}

fun UserErrorCode.toHttpStatus() = when (this) {
    UserErrorCode.NOT_FOUND -> HttpStatusCode.NotFound
    UserErrorCode.USERNAME_TAKEN -> HttpStatusCode.Conflict
    UserErrorCode.USERNAME_TOO_SHORT,
    UserErrorCode.USERNAME_TOO_LONG,
    UserErrorCode.USERNAME_INVALID_CHARS,
    -> HttpStatusCode.UnprocessableEntity
}

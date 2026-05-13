package dev.roasti.features.auth

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import dev.roasti.common.api.ApiError

enum class AuthErrorCode {
    INVALID_CREDENTIALS,
    INVALID_REFRESH_TOKEN,
    USER_DISABLED,
    EMAIL_TAKEN,
    USERNAME_TAKEN,
    USERNAME_TOO_SHORT,
    USERNAME_TOO_LONG,
    USERNAME_INVALID_CHARS,
    PASSWORD_TOO_SHORT,
    PASSWORD_TOO_LONG,
}

@Serializable
data class AuthError(
    @SerialName("code") override val code: AuthErrorCode,
    @SerialName("message") override val message: String,
) : ApiError

fun AuthErrorCode.toError() = when (this) {
    AuthErrorCode.INVALID_CREDENTIALS -> AuthError(this, "Invalid credentials")
    AuthErrorCode.INVALID_REFRESH_TOKEN -> AuthError(this, "Invalid or expired refresh token")
    AuthErrorCode.USER_DISABLED -> AuthError(this, "User account is disabled")
    AuthErrorCode.EMAIL_TAKEN -> AuthError(this, "Email already taken")
    AuthErrorCode.USERNAME_TAKEN -> AuthError(this, "Username already taken")
    AuthErrorCode.USERNAME_TOO_SHORT -> AuthError(this, "Username must be at least 6 characters")
    AuthErrorCode.USERNAME_TOO_LONG -> AuthError(this, "Username must be at most 16 characters")
    AuthErrorCode.USERNAME_INVALID_CHARS -> AuthError(this, "Username can only contain letters, numbers and underscores")
    AuthErrorCode.PASSWORD_TOO_SHORT -> AuthError(this, "Password must be at least 8 characters")
    AuthErrorCode.PASSWORD_TOO_LONG -> AuthError(this, "Password must be at most 32 characters")
}

fun AuthErrorCode.toHttpStatus() = when (this) {
    AuthErrorCode.INVALID_CREDENTIALS,
    AuthErrorCode.INVALID_REFRESH_TOKEN,
    -> HttpStatusCode.Unauthorized
    AuthErrorCode.USER_DISABLED -> HttpStatusCode.Forbidden
    AuthErrorCode.EMAIL_TAKEN,
    AuthErrorCode.USERNAME_TAKEN,
    -> HttpStatusCode.Conflict
    AuthErrorCode.USERNAME_TOO_SHORT,
    AuthErrorCode.USERNAME_TOO_LONG,
    AuthErrorCode.USERNAME_INVALID_CHARS,
    AuthErrorCode.PASSWORD_TOO_SHORT,
    AuthErrorCode.PASSWORD_TOO_LONG,
    -> HttpStatusCode.UnprocessableEntity
}

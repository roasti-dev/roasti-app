package dev.roasti.features.auth

sealed class AuthException(message: String) : Exception(message) {
  data object InvalidCredentials : AuthException("invalid credentials")

  data object InvalidRefreshToken : AuthException("invalid or expired refresh token")

  data object TokenRevoked : AuthException("token revoked")

  data object UserDisabled : AuthException("user account is disabled")

  data object UserNotFound : AuthException("user not found")
}

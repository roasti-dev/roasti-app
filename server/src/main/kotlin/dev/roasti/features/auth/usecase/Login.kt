package dev.roasti.features.auth.usecase

import arrow.core.Either
import arrow.core.raise.either
import dev.roasti.feature.auth.data.network.model.response.AuthResponseDto
import dev.roasti.features.auth.AuthException
import dev.roasti.features.auth.FirebaseSigner
import dev.roasti.features.users.UserRepository
import dev.roasti.features.users.model.Username
import dev.roasti.features.users.toDto

sealed interface LoginError {
  data object InvalidCredentials : LoginError

  data object UserDisabled : LoginError
}

class Login(
    private val userRepo: UserRepository,
    private val signer: FirebaseSigner,
) {
  suspend operator fun invoke(
      username: String,
      password: String,
  ): Either<LoginError, AuthResponseDto> = either {
    val parsedUsername = Username.create(username).mapLeft { LoginError.InvalidCredentials }.bind()

    val user = userRepo.findByUsername(parsedUsername) ?: raise(LoginError.InvalidCredentials)
    val tokens =
        try {
          signer.signInWithPassword(user.email.value, password)
        } catch (e: AuthException) {
          raise(e.toLoginError())
        }
    AuthResponseDto(
        accessToken = tokens.idToken,
        refreshToken = tokens.refreshToken,
        user = user.toDto(),
    )
  }

  private fun AuthException.toLoginError(): LoginError =
      when (this) {
        is AuthException.InvalidCredentials,
        is AuthException.UserNotFound -> LoginError.InvalidCredentials

        is AuthException.UserDisabled -> LoginError.UserDisabled

        else -> LoginError.InvalidCredentials
      }
}

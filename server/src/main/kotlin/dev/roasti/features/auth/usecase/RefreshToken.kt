package dev.roasti.features.auth.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.roasti.feature.auth.data.network.model.response.RefreshResponseDto
import dev.roasti.features.auth.AuthException
import dev.roasti.features.auth.FirebaseSigner
import dev.roasti.features.auth.RevokedTokenRepository

sealed interface RefreshError {
  data object InvalidRefreshToken : RefreshError
}

class RefreshToken(
    private val signer: FirebaseSigner,
    private val revokedTokens: RevokedTokenRepository,
) {
  suspend operator fun invoke(refreshToken: String): Either<RefreshError, RefreshResponseDto> =
      either {
        ensure(!revokedTokens.isRevoked(refreshToken)) { RefreshError.InvalidRefreshToken }
        val tokens =
            try {
              signer.refreshToken(refreshToken)
            } catch (e: AuthException) {
              raise(e.toRefreshError())
            }
        RefreshResponseDto(accessToken = tokens.idToken, refreshToken = tokens.refreshToken)
      }

  private fun AuthException.toRefreshError(): RefreshError =
      when (this) {
        is AuthException.InvalidRefreshToken,
        is AuthException.TokenRevoked -> RefreshError.InvalidRefreshToken

        else -> RefreshError.InvalidRefreshToken
      }
}

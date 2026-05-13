package dev.roasti.features.auth.usecase

import dev.roasti.features.auth.RevokedTokenRepository

class Logout(private val revokedTokens: RevokedTokenRepository) {
  suspend operator fun invoke(refreshToken: String) {
    revokedTokens.add(refreshToken)
  }
}

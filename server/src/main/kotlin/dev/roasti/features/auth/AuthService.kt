package dev.roasti.features.auth

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import com.google.firebase.auth.AuthErrorCode as FirebaseAuthErrorCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserRecord
import dev.roasti.common.ValidationResult
import dev.roasti.common.api.FieldError
import dev.roasti.feature.auth.data.network.model.request.RegisterRequestDto
import dev.roasti.feature.auth.data.network.model.response.AuthResponseDto
import dev.roasti.feature.auth.data.network.model.response.RefreshResponseDto
import dev.roasti.features.users.UserRepository
import dev.roasti.features.users.model.Email
import dev.roasti.features.users.model.FirebaseId
import dev.roasti.features.users.model.User
import dev.roasti.features.users.model.UserId
import dev.roasti.features.users.model.Username
import dev.roasti.features.users.toDto
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val PASSWORD_MIN_LENGTH = 8
private const val PASSWORD_MAX_LENGTH = 32

sealed interface RegisterError {
  data object UsernameTaken : RegisterError

  data object EmailTaken : RegisterError

  data class InvalidInput(val errors: NonEmptyList<FieldError>) : RegisterError
}

sealed interface LoginError {
  data object InvalidCredentials : LoginError

  data object UserDisabled : LoginError
}

sealed interface RefreshError {
  data object InvalidRefreshToken : RefreshError
}

interface AuthService {
  suspend fun register(request: RegisterRequestDto): Either<RegisterError, AuthResponseDto>

  suspend fun login(username: String, password: String): Either<LoginError, AuthResponseDto>

  suspend fun refresh(refreshToken: String): Either<RefreshError, RefreshResponseDto>

  suspend fun logout(refreshToken: String)
}

class AuthServiceImpl(
    private val userRepo: UserRepository,
    private val signer: FirebaseSigner,
    private val revokedTokens: RevokedTokenRepository,
    private val firebaseAuth: FirebaseAuth,
) : AuthService {

  @OptIn(ExperimentalUuidApi::class)
  override suspend fun register(
      request: RegisterRequestDto
  ): Either<RegisterError, AuthResponseDto> {
    val validation =
        Either.zipOrAccumulate(
                { e1, e2 -> e1 + e2 },
                Username.create(request.username),
                Email.create(request.email),
                validatePassword(request.password),
            ) { username, email, _ ->
              Pair(username, email)
            }
            .mapLeft { RegisterError.InvalidInput(it) }

    return either {
      val (username, email) = validation.bind()

      ensure(!userRepo.existsByUsername(username)) { RegisterError.UsernameTaken }
      ensure(!userRepo.existsByEmail(email)) { RegisterError.EmailTaken }

      val firebaseUser =
          try {
            firebaseAuth.createUser(
                UserRecord.CreateRequest().setEmail(email.value).setPassword(request.password)
            )
          } catch (e: FirebaseAuthException) {
            when (e.authErrorCode) {
              FirebaseAuthErrorCode.EMAIL_ALREADY_EXISTS -> raise(RegisterError.EmailTaken)
              else -> throw e
            }
          }

      val id = Uuid.random()
      val customClaims = mapOf("id" to id.toString())
      try {
        firebaseAuth.updateUser(firebaseUser.updateRequest().setCustomClaims(customClaims))
      } catch (e: FirebaseAuthException) {
        when (e.authErrorCode) {
          FirebaseAuthErrorCode.EMAIL_ALREADY_EXISTS -> raise(RegisterError.EmailTaken)
          else -> throw e
        }
      }

      val user =
          userRepo.create(
              User(
                  id = UserId(id),
                  firebaseId = FirebaseId(firebaseUser.uid),
                  email = email,
                  username = username,
                  name = request.name,
                  avatarId = request.avatarId,
                  bio = request.bio,
                  createdAt = Clock.System.now(),
              )
          )

      val tokens = signer.signInWithPassword(email.value, request.password)
      AuthResponseDto(
          accessToken = tokens.idToken,
          refreshToken = tokens.refreshToken,
          user = user.toDto(),
      )
    }
  }

  override suspend fun login(
      username: String,
      password: String,
  ): Either<LoginError, AuthResponseDto> = either {
    val username = Username.create(username).mapLeft { LoginError.InvalidCredentials }.bind()

    val user = userRepo.findByUsername(username) ?: raise(LoginError.InvalidCredentials)
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

  override suspend fun refresh(refreshToken: String): Either<RefreshError, RefreshResponseDto> =
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

  override suspend fun logout(refreshToken: String) {
    revokedTokens.add(refreshToken)
  }

  private fun validatePassword(password: String): ValidationResult<Unit> =
      either {
            zipOrAccumulate(
                { ensure(password.length >= PASSWORD_MIN_LENGTH) { "password too short" } },
                { ensure(password.length <= PASSWORD_MAX_LENGTH) { "password too long" } },
            ) { _, _ ->
            }
          }
          .mapLeft { it.map { msg -> FieldError("password", msg) } }

  private fun AuthException.toLoginError(): LoginError =
      when (this) {
        is AuthException.InvalidCredentials,
        is AuthException.UserNotFound -> LoginError.InvalidCredentials

        is AuthException.UserDisabled -> LoginError.UserDisabled

        else -> LoginError.InvalidCredentials
      }

  private fun AuthException.toRefreshError(): RefreshError =
      when (this) {
        is AuthException.InvalidRefreshToken,
        is AuthException.TokenRevoked -> RefreshError.InvalidRefreshToken

        else -> RefreshError.InvalidRefreshToken
      }
}

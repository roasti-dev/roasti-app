package dev.roasti.features.auth

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserRecord
import com.google.firebase.auth.AuthErrorCode as FirebaseAuthErrorCode
import dev.roasti.features.users.model.UserId
import dev.roasti.features.users.UserRepository
import dev.roasti.feature.auth.data.network.model.request.RegisterRequestDto
import dev.roasti.feature.auth.data.network.model.response.AuthResponseDto
import dev.roasti.feature.auth.data.network.model.response.RefreshResponseDto
import dev.roasti.features.users.model.Email
import dev.roasti.features.users.model.EmailError
import dev.roasti.features.users.model.FirebaseId
import dev.roasti.features.users.model.User
import dev.roasti.features.users.model.Username
import dev.roasti.features.users.model.UsernameError
import dev.roasti.features.users.toDto
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val PASSWORD_MIN_LENGTH = 8
private const val PASSWORD_MAX_LENGTH = 32

sealed interface RegisterError {
    data object UsernameTaken : RegisterError
    data object EmailTaken : RegisterError

    data class InvalidUsername(val error: UsernameError) : RegisterError
    data class InvalidEmail(val error: EmailError) : RegisterError
    data class InvalidPassword(val error: PasswordError) : RegisterError
}

sealed interface LoginError {
    data object InvalidCredentials : LoginError
    data object UserDisabled : LoginError
}

sealed interface RefreshError {
    data object InvalidRefreshToken : RefreshError
}

data class PasswordError(val message: String)

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
    override suspend fun register(request: RegisterRequestDto): Either<RegisterError, AuthResponseDto> =
        either {
            validatePassword(request.password).mapLeft { RegisterError.InvalidPassword(it) }.bind()
            val username = Username.create(request.username)
                .mapLeft { RegisterError.InvalidUsername(it) }.bind()
            val email = Email.create(request.email)
                .mapLeft { RegisterError.InvalidEmail(it) }.bind()

            ensure(!userRepo.existsByUsername(username)) { RegisterError.UsernameTaken }
            ensure(!userRepo.existsByEmail(email)) { RegisterError.EmailTaken }

            val firebaseUser = try {
                firebaseAuth.createUser(
                    UserRecord.CreateRequest()
                        .setEmail(email.value)
                        .setPassword(request.password)
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

            val user = userRepo.create(
                User(
                    id = UserId(id),
                    firebaseId = FirebaseId(firebaseUser.uid),
                    email = email,
                    username = username,
                    name = request.name,
                    avatarId = request.avatarId,
                    bio = request.bio,
                    createdAt = Clock.System.now()
                )
            )

            val tokens = signer.signInWithPassword(email.value, request.password)
            AuthResponseDto(
                accessToken = tokens.idToken,
                refreshToken = tokens.refreshToken,
                user = user.toDto(),
            )
        }

    override suspend fun login(
        username: String,
        password: String
    ): Either<LoginError, AuthResponseDto> = either {

        val username = Username.create(username)
            .mapLeft { LoginError.InvalidCredentials }.bind()

        val user =
            userRepo.findByUsername(username) ?: raise(LoginError.InvalidCredentials)
        val tokens = try {
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
            val tokens = try {
                signer.refreshToken(refreshToken)
            } catch (e: AuthException) {
                raise(e.toRefreshError())
            }
            RefreshResponseDto(
                accessToken = tokens.idToken,
                refreshToken = tokens.refreshToken,
            )
        }

    override suspend fun logout(refreshToken: String) {
        revokedTokens.add(refreshToken)
    }

    private fun validatePassword(password: String): Either<PasswordError, Unit> = either {
        ensure(password.length >= PASSWORD_MIN_LENGTH) { PasswordError("password too short") }
        ensure(password.length <= PASSWORD_MAX_LENGTH) { PasswordError("password too long") }
    }

    private fun AuthException.toLoginError(): LoginError = when (this) {
        is AuthException.InvalidCredentials,
        is AuthException.UserNotFound -> LoginError.InvalidCredentials

        is AuthException.UserDisabled -> LoginError.UserDisabled

        else -> LoginError.InvalidCredentials
    }

    private fun AuthException.toRefreshError(): RefreshError = when (this) {
        is AuthException.InvalidRefreshToken,
        is AuthException.TokenRevoked -> RefreshError.InvalidRefreshToken

        else -> RefreshError.InvalidRefreshToken
    }
}

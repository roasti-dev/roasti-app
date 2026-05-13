package dev.roasti.features.auth

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.context.bind
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserRecord
import com.google.firebase.auth.AuthErrorCode as FirebaseAuthErrorCode
import dev.roasti.features.users.CreateUserInput
import dev.roasti.features.users.UserId
import dev.roasti.features.users.UserRepository
import dev.roasti.features.users.UserService
import dev.roasti.feature.auth.data.network.model.request.RegisterRequestDto
import dev.roasti.feature.auth.data.network.model.response.AuthResponseDto
import dev.roasti.feature.auth.data.network.model.response.RefreshResponseDto
import dev.roasti.features.users.FirebaseId
import dev.roasti.features.users.UsernameValidationError
import dev.roasti.features.users.toDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val PASSWORD_MIN_LENGTH = 8
private const val PASSWORD_MAX_LENGTH = 32

sealed interface RegisterError {
    data object UsernameTaken : RegisterError
    data object EmailTaken : RegisterError

    data class InvalidUsername(val error: UsernameValidationError) : RegisterError
    data class InvalidPassword(val error: PasswordValidationError) : RegisterError
}

sealed interface LoginError {
    data object InvalidCredentials : LoginError
    data object UserDisabled : LoginError
}

sealed interface RefreshError {
    data object InvalidRefreshToken : RefreshError
}

data class PasswordValidationError(val message: String)

interface AuthService {
    suspend fun register(request: RegisterRequestDto): Either<RegisterError, AuthResponseDto>
    suspend fun login(username: String, password: String): Either<LoginError, AuthResponseDto>
    suspend fun refresh(refreshToken: String): Either<RefreshError, RefreshResponseDto>
    suspend fun logout(refreshToken: String)
}

class AuthServiceImpl(
    private val userRepo: UserRepository,
    private val userService: UserService,
    private val signer: FirebaseSigner,
    private val revokedTokens: RevokedTokenRepository,
    private val firebaseAuth: FirebaseAuth,
) : AuthService {

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun register(request: RegisterRequestDto): Either<RegisterError, AuthResponseDto> =
        either {
            validatePassword(request.password).mapLeft { RegisterError.InvalidPassword(it) }.bind()
            userService.validateUsername(request.username)
                .mapLeft { RegisterError.InvalidUsername(it) }.bind()

            ensure(!userRepo.existsByUsername(request.username)) { RegisterError.UsernameTaken }
            ensure(!userRepo.existsByEmail(request.email)) { RegisterError.EmailTaken }

            val firebaseUser = try {
                firebaseAuth.createUser(
                    UserRecord.CreateRequest()
                        .setEmail(request.email)
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
                CreateUserInput(
                    id = UserId(id),
                    firebaseId = FirebaseId(firebaseUser.uid),
                    email = request.email,
                    username = request.username,
                    name = request.name,
                    avatarId = request.avatarId,
                    bio = request.bio,
                )
            )

            val tokens = signer.signInWithPassword(request.email, request.password)
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
        val user =
            userRepo.findByUsername(username) ?: raise(LoginError.InvalidCredentials)
        val tokens = try {
            signer.signInWithPassword(user.email, password)
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

    private fun validatePassword(password: String): Either<PasswordValidationError, Unit> = either {
        ensure(password.length >= PASSWORD_MIN_LENGTH) { PasswordValidationError("password too short") }
        ensure(password.length <= PASSWORD_MAX_LENGTH) { PasswordValidationError("password too long") }
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

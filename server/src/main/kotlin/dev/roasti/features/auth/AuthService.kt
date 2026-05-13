package dev.roasti.features.auth

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserRecord
import com.google.firebase.auth.AuthErrorCode as FirebaseAuthErrorCode
import dev.roasti.features.users.CreateUserInput
import dev.roasti.features.users.UserId
import dev.roasti.features.users.UserErrorCode
import dev.roasti.features.users.UserRepository
import dev.roasti.features.users.UserService
import dev.roasti.feature.auth.data.network.model.request.RegisterRequestDto
import dev.roasti.feature.auth.data.network.model.response.AuthResponseDto
import dev.roasti.feature.auth.data.network.model.response.RefreshResponseDto
import dev.roasti.features.users.FirebaseId
import dev.roasti.features.users.toDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val PASSWORD_MIN_LENGTH = 8
private const val PASSWORD_MAX_LENGTH = 32

interface AuthService {
    suspend fun register(request: RegisterRequestDto): Either<AuthErrorCode, AuthResponseDto>
    suspend fun login(username: String, password: String): Either<AuthErrorCode, AuthResponseDto>
    suspend fun refresh(refreshToken: String): Either<AuthErrorCode, RefreshResponseDto>
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
    override suspend fun register(request: RegisterRequestDto): Either<AuthErrorCode, AuthResponseDto> {
        validatePassword(request.password)?.let { return it.left() }
        userService.validateUsername(request.username)?.let { return it.toAuthErrorCode().left() }

        if (userRepo.existsByUsername(request.username)) return AuthErrorCode.USERNAME_TAKEN.left()
        if (userRepo.existsByEmail(request.email)) return AuthErrorCode.EMAIL_TAKEN.left()

        val firebaseUser = try {
            firebaseAuth.createUser(
                UserRecord.CreateRequest()
                    .setEmail(request.email)
                    .setPassword(request.password)
            )
        } catch (e: FirebaseAuthException) {
            return when (e.authErrorCode) {
                FirebaseAuthErrorCode.EMAIL_ALREADY_EXISTS -> AuthErrorCode.EMAIL_TAKEN.left()
                else -> throw e
            }
        }

        val id = Uuid.random()
        val customClaims = mapOf("id" to id.toString())
        try {
            firebaseAuth.updateUser(firebaseUser.updateRequest().setCustomClaims(customClaims))
        } catch (e: FirebaseAuthException) {
            return when (e.authErrorCode) {
                FirebaseAuthErrorCode.EMAIL_ALREADY_EXISTS -> AuthErrorCode.EMAIL_TAKEN.left()
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
        return AuthResponseDto(
            accessToken = tokens.idToken,
            refreshToken = tokens.refreshToken,
            user = user.toDto(),
        ).right()
    }

    override suspend fun login(username: String, password: String): Either<AuthErrorCode, AuthResponseDto> {
        val user = userRepo.findByUsername(username) ?: return AuthErrorCode.INVALID_CREDENTIALS.left()
        val tokens = try {
            signer.signInWithPassword(user.email, password)
        } catch (e: AuthException) {
            return e.toErrorCode().left()
        }
        return AuthResponseDto(
            accessToken = tokens.idToken,
            refreshToken = tokens.refreshToken,
            user = user.toDto(),
        ).right()
    }

    override suspend fun refresh(refreshToken: String): Either<AuthErrorCode, RefreshResponseDto> {
        if (revokedTokens.isRevoked(refreshToken)) return AuthErrorCode.INVALID_REFRESH_TOKEN.left()
        val tokens = try {
            signer.refreshToken(refreshToken)
        } catch (e: AuthException) {
            return e.toErrorCode().left()
        }
        return RefreshResponseDto(
            accessToken = tokens.idToken,
            refreshToken = tokens.refreshToken,
        ).right()
    }

    override suspend fun logout(refreshToken: String) {
        revokedTokens.add(refreshToken)
    }

    private fun validatePassword(password: String): AuthErrorCode? = when {
        password.length < PASSWORD_MIN_LENGTH -> AuthErrorCode.PASSWORD_TOO_SHORT
        password.length > PASSWORD_MAX_LENGTH -> AuthErrorCode.PASSWORD_TOO_LONG
        else -> null
    }

    private fun AuthException.toErrorCode(): AuthErrorCode = when (this) {
        is AuthException.InvalidCredentials -> AuthErrorCode.INVALID_CREDENTIALS
        is AuthException.InvalidRefreshToken -> AuthErrorCode.INVALID_REFRESH_TOKEN
        is AuthException.UserDisabled -> AuthErrorCode.USER_DISABLED
        is AuthException.UserNotFound -> AuthErrorCode.INVALID_CREDENTIALS
        is AuthException.TokenRevoked -> AuthErrorCode.INVALID_REFRESH_TOKEN
    }

    private fun UserErrorCode.toAuthErrorCode(): AuthErrorCode = when (this) {
        UserErrorCode.USERNAME_TOO_SHORT -> AuthErrorCode.USERNAME_TOO_SHORT
        UserErrorCode.USERNAME_TOO_LONG -> AuthErrorCode.USERNAME_TOO_LONG
        UserErrorCode.USERNAME_INVALID_CHARS -> AuthErrorCode.USERNAME_INVALID_CHARS
        else -> error("unexpected UserErrorCode in auth context: $this")
    }
}

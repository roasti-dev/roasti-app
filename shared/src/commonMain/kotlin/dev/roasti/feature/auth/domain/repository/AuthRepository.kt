package dev.roasti.feature.auth.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import dev.roasti.feature.auth.domain.model.AuthState
import dev.roasti.feature.auth.domain.model.User

interface AuthRepository {
    val authState: StateFlow<AuthState>

    fun getUser(): Flow<User?>

    suspend fun bootstrap()

    suspend fun login(
        username: String,
        password: String,
    ): Result<Unit>

    suspend fun register(
        username: String,
        email: String,
        password: String,
        bio: String?,
        avatarId: String?,
    ): Result<Unit>

    suspend fun logout()

    suspend fun syncProfile(): Result<User>

    suspend fun updateProfile(imageId: String? = null, bio: String? = null, username: String? = null): Result<User>
}

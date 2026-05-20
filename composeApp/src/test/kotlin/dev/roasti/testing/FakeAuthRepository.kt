package dev.roasti.testing

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import dev.roasti.feature.auth.domain.model.AuthState
import dev.roasti.feature.auth.domain.model.PublicUserProfile
import dev.roasti.feature.auth.domain.model.User
import dev.roasti.feature.auth.domain.repository.AuthRepository

class FakeAuthRepository(initialUser: User? = null) : AuthRepository {

    private val userFlow = MutableStateFlow(initialUser)

    fun setUser(user: User?) { userFlow.value = user }

    override val authState: StateFlow<AuthState> = MutableStateFlow(AuthState.Guest)

    override fun getUser(): Flow<User?> = userFlow

    override suspend fun bootstrap() = Unit
    override suspend fun login(username: String, password: String): Result<Unit> = Result.success(Unit)
    override suspend fun register(
        username: String, email: String, password: String, bio: String?, avatarId: String?,
    ): Result<Unit> = Result.success(Unit)
    override suspend fun logout() = Unit
    override suspend fun syncProfile(): Result<User> = Result.failure(NotImplementedError())
    override suspend fun updateProfile(imageId: String?, bio: String?, username: String?): Result<User> =
        Result.failure(NotImplementedError())

    override suspend fun getPublicUserProfile(username: String): Result<PublicUserProfile> = Result.failure(NotImplementedError())
}

fun fakeUser(id: String = "user-1", username: String = "tester"): User =
    User(id = id, username = username, bio = null, avatarId = null, email = "$username@test.local")

package dev.roasti.feature.auth.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import dev.roasti.User
import dev.roasti.UserQueries

class UserCacheDataSource(private val userQueries: UserQueries) {
    fun getUserFlow(): Flow<User?> =
        userQueries.getUser().asFlow().mapToOneOrNull(Dispatchers.Default)

    suspend fun getUser(): User? =
        withContext(Dispatchers.Default) { userQueries.getUser().executeAsOneOrNull() }

    suspend fun saveUser(
        id: String,
        imageId: String?,
        bio: String?,
        username: String,
        email: String
    ) =
        withContext(Dispatchers.Default) {
            userQueries.upsertUser(
                id = id,
                image_id = imageId,
                bio = bio,
                username = username,
                email = email,
            )
        }

    suspend fun deleteUser() = withContext(Dispatchers.Default) {
        userQueries.deleteUser()
    }
}
package dev.roasti.feature.auth.data.session

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dev.roasti.feature.auth.data.network.AuthApiClient
import dev.roasti.feature.auth.data.network.mapper.toDomain
import dev.roasti.core.session.SessionRefresher
import dev.roasti.core.session.SessionRepository
import dev.roasti.core.session.UserSession

class TokenRefreshCoordinator(
    private val authApiClient: AuthApiClient,
    private val sessionRepository: SessionRepository,
) : SessionRefresher {

    private val refreshMutex = Mutex()

    override suspend fun refreshSession(failedAccessToken: String): Result<UserSession> = refreshMutex.withLock {
        val currentSession = sessionRepository.currentSession()
            ?: return@withLock Result.failure(IllegalStateException("Missing session for refresh"))

        if (currentSession.accessToken != failedAccessToken) {
            return@withLock Result.success(currentSession)
        }

        val refreshedSession = authApiClient.refresh(currentSession.refreshToken)
            .map { it.toDomain() }

        if (refreshedSession.isSuccess) {
            sessionRepository.saveSession(refreshedSession.getOrThrow())
        } else if (refreshedSession.exceptionOrNull().isUnauthorizedError()) {
            sessionRepository.clearSession()
        }

        return@withLock refreshedSession
    }
}

private fun Throwable?.isUnauthorizedError(): Boolean {
    val clientRequestException = this as? ClientRequestException ?: return false
    return clientRequestException.response.status == HttpStatusCode.Unauthorized
}

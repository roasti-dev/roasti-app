package dev.roasti.core.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import dev.roasti.core.session.SessionRefresher
import dev.roasti.core.session.SessionRepository

class AuthorizedRequestExecutor(
    private val sessionRepository: SessionRepository,
    private val sessionRefresher: SessionRefresher,
) {

    suspend fun <T> execute(block: suspend (accessToken: String) -> T): Result<T> = runCatching {
        executeOrThrow(block)
    }

    private suspend fun <T> executeOrThrow(block: suspend (accessToken: String) -> T): T {
        val currentSession = sessionRepository.currentSession() ?: error("Authorized request requires session")

        return try {
            block(currentSession.accessToken)
        } catch (error: ClientRequestException) {
            if (error.response.status != HttpStatusCode.Unauthorized) {
                throw error
            }

            val refreshedSession = sessionRefresher.refreshSession(currentSession.accessToken).getOrThrow()
            block(refreshedSession.accessToken)
        }
    }
}

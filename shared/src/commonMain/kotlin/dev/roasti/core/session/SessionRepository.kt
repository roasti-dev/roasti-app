package dev.roasti.core.session

import kotlinx.coroutines.flow.StateFlow

interface SessionRepository {
    val authState: StateFlow<SessionState>

    suspend fun restore()

    suspend fun saveSession(session: UserSession)

    suspend fun clearSession()

    fun currentSession(): UserSession?
}

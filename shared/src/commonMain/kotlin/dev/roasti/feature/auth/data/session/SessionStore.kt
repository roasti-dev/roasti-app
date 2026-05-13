package dev.roasti.feature.auth.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dev.roasti.core.session.SessionRepository
import dev.roasti.core.session.SessionState
import dev.roasti.core.session.UserSession
import dev.roasti.core.session.storage.TokenStorage
import dev.roasti.core.session.storage.TokensDto

class SessionStore(
    private val tokenStorage: TokenStorage,
) : SessionRepository {

    private val mutableAuthState = MutableStateFlow<SessionState>(SessionState.Empty)

    override val authState: StateFlow<SessionState> = mutableAuthState.asStateFlow()

    override suspend fun restore() {
        val tokens = tokenStorage.readTokens()
        mutableAuthState.value = if (tokens != null) {
            SessionState.Authenticated(UserSession(tokens.accessToken, tokens.refreshToken))
        } else {
            SessionState.Guest
        }
    }

    override suspend fun saveSession(session: UserSession) {
        tokenStorage.writeTokens(TokensDto(session.accessToken, session.refreshToken))
        mutableAuthState.value = session.toAuthenticatedState()
    }

    override suspend fun clearSession() {
        tokenStorage.clearTokens()
        mutableAuthState.value = SessionState.Guest
    }

    override fun currentSession(): UserSession? = (authState.value as? SessionState.Authenticated)?.session
}

private fun UserSession.toAuthenticatedState() = SessionState.Authenticated(this)

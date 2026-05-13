package dev.roasti.core.session

sealed interface SessionState {
    data object Empty : SessionState
    data object Guest : SessionState
    data class Error(val message: String) : SessionState
    data class Authenticated(val session: UserSession) : SessionState
}

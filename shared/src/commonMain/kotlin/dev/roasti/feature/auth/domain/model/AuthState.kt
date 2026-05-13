package dev.roasti.feature.auth.domain.model

sealed interface AuthState {
    data object Loading : AuthState
    data object Guest : AuthState
    data class Error(val message: String): AuthState
    data class Authenticated(val user: User) : AuthState
}


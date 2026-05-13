package dev.roasti.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dev.roasti.feature.auth.domain.model.AuthState
import dev.roasti.feature.auth.domain.repository.AuthRepository

class AppNavigationViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState

    private var hasBootstrapped = false

    fun bootstrap() {
        if (hasBootstrapped) {
            return
        }

        hasBootstrapped = true
        viewModelScope.launch {
            authRepository.bootstrap()
        }
    }
}

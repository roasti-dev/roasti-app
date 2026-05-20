package dev.roasti.ui.features.userprofile

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import dev.roasti.feature.auth.domain.repository.AuthRepository
import dev.roasti.ui.features.userprofile.mapper.toUiModel
import dev.roasti.ui.uikit.state.ContentUiState
import dev.roasti.ui.uikit.state.UiError
import dev.roasti.utils.stateInWhileSubscribe

@OptIn(ExperimentalCoroutinesApi::class)
class UserProfileViewModel(
    private val userId: String,
    private val username: String,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val reloadTrigger = MutableStateFlow(false)

    val state: StateFlow<UserProfileUiState> = reloadTrigger
        .flatMapLatest { loadProfile() }
        .stateInWhileSubscribe(ContentUiState.Loading)

    fun onRetry() {
        reloadTrigger.update { !it }
    }

    private fun loadProfile(): Flow<UserProfileUiState> = flow {
        emit(ContentUiState.Loading)
        emit(
            authRepository.getPublicUserProfile(username).fold(
                onSuccess = { ContentUiState.Content(it.toUiModel()) },
                onFailure = { ContentUiState.FullscreenError(UiError.Generic) },
            )
        )
    }
}

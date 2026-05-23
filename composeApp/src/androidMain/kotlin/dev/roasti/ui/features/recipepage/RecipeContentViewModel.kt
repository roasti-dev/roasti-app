package dev.roasti.ui.features.recipepage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dev.roasti.feature.auth.domain.repository.AuthRepository
import dev.roasti.feature.recipe.domain.RecipeRepository
import dev.roasti.ui.features.recipepage.mapper.toUiModel
import dev.roasti.ui.features.recipepage.model.RecipeDetailsUiModel
import dev.roasti.ui.uikit.state.ContentUiState
import dev.roasti.ui.uikit.state.UiError
import dev.roasti.ui.uikit.state.UiEvent

class RecipeContentViewModel(
    private val recipeId: String,
    private val repository: RecipeRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val refreshStatus = MutableStateFlow<RefreshStatus>(RefreshStatus.Idle)

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val _navEvents = MutableSharedFlow<RecipeContentNavEvent>(extraBufferCapacity = 1)
    val navEvents: SharedFlow<RecipeContentNavEvent> = _navEvents.asSharedFlow()

    val state: StateFlow<ContentUiState<RecipeDetailsUiModel>> = combine(
        repository.observeById(recipeId),
        authRepository.getUser(),
        refreshStatus,
    ) { cache, currentUser, status ->
        val cacheUi = cache?.toUiModel(currentUserId = currentUser?.id)
        when {
            cacheUi != null -> ContentUiState.Content(
                data = cacheUi,
                isRefreshing = status is RefreshStatus.Loading,
            )
            status is RefreshStatus.Failed -> ContentUiState.FullscreenError(status.error)
            else -> ContentUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ContentUiState.Loading,
    )

    init {
        retry()
    }

    fun retry() {
        viewModelScope.launch {
            refreshStatus.value = RefreshStatus.Loading
            repository.refreshById(recipeId).fold(
                onSuccess = { refreshStatus.value = RefreshStatus.Idle },
                onFailure = {
                    val hasCache = repository.observeById(recipeId).first() != null
                    if (hasCache) {
                        refreshStatus.value = RefreshStatus.Idle
                        _events.tryEmit(UiEvent.ShowError(UiError.Generic))
                    } else {
                        refreshStatus.value = RefreshStatus.Failed(UiError.Generic)
                    }
                },
            )
        }
    }

    fun toggleLike() {
        viewModelScope.launch {
            repository.toggleLike(recipeId).onFailure {
                _events.tryEmit(UiEvent.ShowError(UiError.Generic))
            }
        }
    }

    fun onRemoveRecipe() {
        viewModelScope.launch {
            repository.removeRecipe(recipeId).fold(
                onSuccess = { _navEvents.tryEmit(RecipeContentNavEvent.NavigateBack) },
                onFailure = { _events.tryEmit(UiEvent.ShowError(UiError.Generic)) },
            )
        }
    }

    private sealed interface RefreshStatus {
        data object Idle : RefreshStatus
        data object Loading : RefreshStatus
        data class Failed(val error: UiError) : RefreshStatus
    }
}

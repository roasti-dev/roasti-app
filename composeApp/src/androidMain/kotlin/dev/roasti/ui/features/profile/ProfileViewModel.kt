package dev.roasti.ui.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.roasti.feature.auth.domain.model.User
import dev.roasti.feature.auth.domain.repository.AuthRepository
import dev.roasti.feature.likes.data.LikesApiClient
import dev.roasti.feature.likes.data.toDomain
import dev.roasti.feature.upload.domain.UploadRepository
import dev.roasti.ui.features.favorites.model.FavoritesPreviewState
import dev.roasti.ui.features.recipelist.mapper.toUiModel
import dev.roasti.ui.uikit.state.ContentUiState
import dev.roasti.ui.uikit.state.UiError
import dev.roasti.ui.uikit.state.UiEvent
import dev.roasti.utils.stateInWhileSubscribe

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val uploadRepository: UploadRepository,
    private val likesApiClient: LikesApiClient,
) : ViewModel(), ProfileRowListener {

    private val refreshStatus = MutableStateFlow<RefreshStatus>(RefreshStatus.Idle)
    private val isLoggingOut = MutableStateFlow(false)
    private val favoritesRefreshTrigger = MutableStateFlow(0)
    private val isUserImageUploadProgressFlow = MutableStateFlow(false)

    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private val userStatisticsState: StateFlow<ProfileStatisticsUiModel> =
        MutableStateFlow(ProfileStatisticsUiModel.empty()).asStateFlow()

    private val userState: StateFlow<ProfileUserUiModel?> = authRepository.getUser()
        .combine(isUserImageUploadProgressFlow) { user, isImageLoading -> user?.toUi(isImageLoading) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val favoritesState: StateFlow<FavoritesPreviewState> = combine(
        authRepository.getUser(),
        favoritesRefreshTrigger,
    ) { user, _ -> user?.id }
        .flatMapLatest { userId -> favoriteRecipesFlow(userId) }
        .stateInWhileSubscribe(FavoritesPreviewState.Loading)

    val state: StateFlow<ContentUiState<ProfileState>> = combine(
        userState,
        userStatisticsState,
        favoritesState,
        refreshStatus,
    ) { user, statistics, favorites, status ->
        when {
            user != null -> ContentUiState.Content(
                data = ProfileState(user = user, statistics = statistics, favoritesState = favorites),
                isRefreshing = status is RefreshStatus.Loading,
            )
            status is RefreshStatus.Failed -> ContentUiState.FullscreenError(status.error)
            else -> ContentUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ContentUiState.Loading)

    init {
        retry()
    }

    fun retry() {
        viewModelScope.launch {
            refreshStatus.value = RefreshStatus.Loading
            authRepository.syncProfile().fold(
                onSuccess = {
                    favoritesRefreshTrigger.update { it + 1 }
                    refreshStatus.value = RefreshStatus.Idle
                },
                onFailure = {
                    if (userState.value != null) {
                        refreshStatus.value = RefreshStatus.Idle
                        _events.tryEmit(UiEvent.ShowError(UiError.Generic))
                    } else {
                        refreshStatus.value = RefreshStatus.Failed(UiError.Generic)
                    }
                },
            )
        }
    }

    private fun favoriteRecipesFlow(userId: String?) = flow {
        if (userId == null) {
            emit(FavoritesPreviewState.Empty)
            return@flow
        }

        val itemsLimit = 20
        val maxVisibleLimit = itemsLimit - 1
        val result = likesApiClient.getLikedRecipes(userId = userId, limit = itemsLimit, page = 1)
            .map { it.toDomain() }

        val likes = result.getOrNull()
        if (!likes?.items.isNullOrEmpty()) {
            emit(
                FavoritesPreviewState.Content(
                    items = likes.items.map { it.recipe.toUiModel() }.take(maxVisibleLimit),
                    hasMore = likes.items.size > maxVisibleLimit,
                )
            )
        } else {
            emit(FavoritesPreviewState.Empty)
        }
    }

    private fun logout() {
        if (isLoggingOut.value) return
        viewModelScope.launch {
            isLoggingOut.value = true
            authRepository.logout()
            isLoggingOut.value = false
        }
    }

    override fun onSettingsClick() {
        // to be implemented
    }

    override fun onEditClick() {
        // to be implemented
    }

    override fun onImagePicked(fileName: String, bytes: ByteArray) {
        viewModelScope.launch {
            isUserImageUploadProgressFlow.update { true }
            uploadImage(fileName, bytes).onFailure {
                _events.tryEmit(UiEvent.ShowError(UiError.Generic))
            }
            isUserImageUploadProgressFlow.update { false }
        }
    }

    override fun onLogoutClick() {
        logout()
    }

    private suspend fun uploadImage(fileName: String, bytes: ByteArray): Result<Unit> {
        val uploaded = uploadRepository.uploadImage(fileName, bytes)
            .getOrElse { return Result.failure(it) }
        return authRepository.updateProfile(imageId = uploaded.id).map { }
    }

    private sealed interface RefreshStatus {
        data object Idle : RefreshStatus
        data object Loading : RefreshStatus
        data class Failed(val error: UiError) : RefreshStatus
    }
}

interface ProfileRowListener {
    fun onImagePicked(fileName: String, bytes: ByteArray)
    fun onEditClick()
    fun onSettingsClick()

    fun onLogoutClick()
}

private fun User.toUi(isImageLoading: Boolean) = ProfileUserUiModel(
    imageId = this.avatarId,
    nickname = this.username,
    bio = this.bio,
    email = this.email,
    isImageUploadInProgress = isImageLoading,
)

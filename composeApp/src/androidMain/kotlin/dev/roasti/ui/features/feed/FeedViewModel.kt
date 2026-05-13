package dev.roasti.ui.features.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import dev.roasti.feature.auth.domain.repository.AuthRepository
import dev.roasti.feature.post.data.mapper.toDomain
import dev.roasti.feature.post.data.paging.PagingPostRepository
import dev.roasti.ui.features.feed.mapper.toDomain
import dev.roasti.ui.features.feed.mapper.toUiModel
import dev.roasti.ui.features.feed.model.PostUiModel
import dev.roasti.ui.uikit.post.PostUserReaction
import dev.roasti.utils.stateInWhileSubscribe

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModel(
    private val pagingPostRepository: PagingPostRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val currentUserIdFlow: Flow<String?> =
        authRepository.getUser().map { it?.id }.distinctUntilChanged()

    private val manualRefreshMutable = MutableStateFlow(false)
    val isManualRefresh: StateFlow<Boolean> = manualRefreshMutable.asStateFlow()

    val hasCachedPosts: StateFlow<Boolean> =
        pagingPostRepository.observeHasCachedPosts()
            .stateInWhileSubscribe(false)

    val pagingPostsState: Flow<PagingData<PostUiModel>> =
        currentUserIdFlow
            .flatMapLatest { userId ->
                pagingPostRepository.getOfflineFirstPostsPager()
                    .map { pagingData -> pagingData.map { it.toDomain().toUiModel(userId) } }
            }
            .cachedIn(viewModelScope)

    fun onRatingChange(post: PostUiModel, intent: PostUserReaction) {
        viewModelScope.launch {
            pagingPostRepository.setVote(post.id, intent.toDomain())
        }
    }

    fun onDeletePost(postId: String) {
        viewModelScope.launch {
            pagingPostRepository.deletePost(postId)
        }
    }

    fun startManualRefresh() {
        manualRefreshMutable.value = true
    }

    fun finishManualRefresh() {
        manualRefreshMutable.value = false
    }
}

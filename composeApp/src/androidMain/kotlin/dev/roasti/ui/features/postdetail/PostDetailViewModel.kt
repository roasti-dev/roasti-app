package dev.roasti.ui.features.postdetail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.roasti.feature.auth.domain.repository.AuthRepository
import dev.roasti.feature.comment.data.paging.PagingCommentRepository
import dev.roasti.feature.post.data.paging.PagingPostRepository
import dev.roasti.ui.features.feed.mapper.toDomain
import dev.roasti.ui.features.feed.mapper.toUiModel
import dev.roasti.ui.features.feed.model.PostUiModel
import dev.roasti.ui.features.postdetail.mapper.toUi
import dev.roasti.ui.features.postdetail.model.CommentThreadUiModel
import dev.roasti.ui.features.postdetail.model.CommentUiModel
import dev.roasti.ui.uikit.post.PostUserReaction
import dev.roasti.utils.stateInWhileSubscribe

class PostDetailViewModel(
    private val postId: String,
    private val pagingPostRepository: PagingPostRepository,
    private val pagingCommentRepository: PagingCommentRepository,
    authRepository: AuthRepository,
) : ViewModel() {

    sealed interface HeaderState {
        data object Loading : HeaderState
        data object Error : HeaderState
        data class Content(val post: PostUiModel) : HeaderState
    }

    sealed interface ComposerMode {
        data object New : ComposerMode
        data class Reply(val parentId: String, val parentAuthor: String?) : ComposerMode
        data class Edit(val commentId: String) : ComposerMode
    }

    @Immutable
    data class ComposerState(
        val mode: ComposerMode = ComposerMode.New,
        val text: String = "",
        val isSubmitting: Boolean = false,
    )

    sealed interface PostDetailEvent {
        data object DeleteSuccess : PostDetailEvent
        data object CreateError : PostDetailEvent
        data object EditError : PostDetailEvent
        data object DeleteCommentError : PostDetailEvent
    }

    private val isHeaderRefreshFailed = MutableStateFlow(false)
    private val eventsChannel = Channel<PostDetailEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    private val composerStateFlow = MutableStateFlow(ComposerState())
    val composer: StateFlow<ComposerState> = composerStateFlow.asStateFlow()

    private val currentUserIdFlow: Flow<String?> =
        authRepository.getUser().map { it?.id }.distinctUntilChanged()

    val headerState: StateFlow<HeaderState> = combine(
        pagingPostRepository.observePostById(postId),
        isHeaderRefreshFailed,
        currentUserIdFlow,
    ) { post, failed, currentUserId ->
        when {
            post != null -> HeaderState.Content(post.toUiModel(currentUserId))
            failed -> HeaderState.Error
            else -> HeaderState.Loading
        }
    }.stateInWhileSubscribe(HeaderState.Loading)

    val commentsPager: Flow<PagingData<CommentThreadUiModel>> = combine(
        pagingCommentRepository.threadsPager(postId).cachedIn(viewModelScope),
        currentUserIdFlow,
    ) { pagingData, userId ->
        pagingData.map { thread -> thread.toUi(userId) }
    }

    init {
        viewModelScope.launch {
            pagingPostRepository.refreshPostById(postId)
                .onFailure { isHeaderRefreshFailed.update { true } }
        }
    }

    fun onRatingChange(intent: PostUserReaction) {
        viewModelScope.launch {
            pagingPostRepository.setVote(postId, intent.toDomain())
        }
    }

    fun onDeletePost() {
        viewModelScope.launch {
            pagingPostRepository.deletePost(postId).onSuccess {
                eventsChannel.send(PostDetailEvent.DeleteSuccess)
            }
        }
    }

    fun onComposerTextChange(text: String) {
        composerStateFlow.update { it.copy(text = text) }
    }

    fun onCancelComposerMode() {
        composerStateFlow.update { ComposerState() }
    }

    fun onStartReply(comment: CommentUiModel) {
        composerStateFlow.update {
            ComposerState(
                mode = ComposerMode.Reply(parentId = comment.id, parentAuthor = comment.authorName),
                text = "",
            )
        }
    }

    fun onStartEdit(comment: CommentUiModel) {
        composerStateFlow.update {
            ComposerState(
                mode = ComposerMode.Edit(commentId = comment.id),
                text = comment.body,
            )
        }
    }

    fun onComposerSubmit() {
        val snapshot = composerStateFlow.value
        if (snapshot.isSubmitting || snapshot.text.isBlank()) return
        composerStateFlow.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val result = when (val mode = snapshot.mode) {
                ComposerMode.New ->
                    pagingCommentRepository.createComment(postId, snapshot.text.trim(), parentId = null)

                is ComposerMode.Reply ->
                    pagingCommentRepository.createComment(postId, snapshot.text.trim(), parentId = mode.parentId)

                is ComposerMode.Edit ->
                    pagingCommentRepository.updateComment(mode.commentId, snapshot.text.trim())
            }
            result
                .onSuccess { composerStateFlow.update { ComposerState() } }
                .onFailure {
                    composerStateFlow.update { it.copy(isSubmitting = false) }
                    val event = when (snapshot.mode) {
                        is ComposerMode.Edit -> PostDetailEvent.EditError
                        else -> PostDetailEvent.CreateError
                    }
                    eventsChannel.send(event)
                }
        }
    }

    fun onDeleteComment(commentId: String) {
        viewModelScope.launch {
            pagingCommentRepository.deleteComment(commentId)
                .onFailure { eventsChannel.send(PostDetailEvent.DeleteCommentError) }
        }
    }
}

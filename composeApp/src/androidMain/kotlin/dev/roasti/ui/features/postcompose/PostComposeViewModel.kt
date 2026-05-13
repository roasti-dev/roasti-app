package dev.roasti.ui.features.postcompose

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.roasti.core.utils.imageUrl
import dev.roasti.feature.post.data.paging.PagingPostRepository
import dev.roasti.feature.upload.domain.UploadRepository

class PostComposeViewModel(
    private val postId: String?,
    private val pagingPostRepository: PagingPostRepository,
    private val uploadRepository: UploadRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        PostComposeUiState(
            mode = if (postId == null) PostComposeMode.CREATE else PostComposeMode.EDIT,
            isLoadingExisting = postId != null,
        )
    )
    val state: StateFlow<PostComposeUiState> = _state.asStateFlow()

    private val eventsChannel = Channel<PostComposeEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    init {
        if (postId != null) {
            loadExistingPost(postId)
        }
    }

    fun onTitleChange(value: String) {
        _state.update { it.copy(title = value, submitError = null) }
    }

    fun onBodyChange(value: String) {
        _state.update { it.copy(body = value, submitError = null) }
    }

    fun onImagePicked(fileName: String, bytes: ByteArray) {
        _state.update { it.copy(photoState = PhotoState.Uploading) }
        viewModelScope.launch {
            uploadRepository.uploadImage(fileName, bytes).fold(
                onSuccess = { uploaded ->
                    _state.update {
                        it.copy(
                            photoState = PhotoState.Ready(
                                imageId = uploaded.id,
                                previewUrl = imageUrl(uploaded.id),
                            )
                        )
                    }
                },
                onFailure = {
                    _state.update { it.copy(photoState = PhotoState.Error) }
                },
            )
        }
    }

    fun onRemoveImage() {
        _state.update { it.copy(photoState = PhotoState.None) }
    }

    fun onSubmit() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(isSubmitting = true, submitError = null) }

        val title = current.title.trim()
        val body = current.body.trim().takeIf { it.isNotEmpty() }

        val imageIds = (current.photoState as? PhotoState.Ready)?.let { listOf(it.imageId) }
            ?: emptyList()

        viewModelScope.launch {
            val result = if (postId == null) {
                pagingPostRepository.createPost(title = title, text = body, imageIds = imageIds)
            } else {
                pagingPostRepository.updatePost(
                    id = postId,
                    title = title,
                    text = body,
                    imageIds = imageIds,
                )
            }
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isSubmitting = false) }
                    eventsChannel.send(PostComposeEvent.SubmitSuccess)
                },
                onFailure = {
                    _state.update { it.copy(isSubmitting = false, submitError = SubmitError) }
                },
            )
        }
    }

    private fun loadExistingPost(id: String) {
        viewModelScope.launch {
            val post = pagingPostRepository.observePostById(id).first { it != null } ?: return@launch
            val photoState = post.images.firstOrNull()?.let { imageId ->
                PhotoState.Ready(imageId = imageId, previewUrl = imageUrl(imageId))
            } ?: PhotoState.None
            _state.update {
                it.copy(
                    title = post.title.orEmpty(),
                    body = post.text,
                    photoState = photoState,
                    isLoadingExisting = false,
                )
            }
        }
    }
}

enum class PostComposeMode { CREATE, EDIT }

data class PostComposeUiState(
    val mode: PostComposeMode = PostComposeMode.CREATE,
    val title: String = "",
    val body: String = "",
    val photoState: PhotoState = PhotoState.None,
    val isLoadingExisting: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitError: SubmitError? = null,
) {
    val canSubmit: Boolean
        get() = !isSubmitting && !isLoadingExisting && title.isNotBlank()
}

sealed interface PhotoState {
    data object None : PhotoState
    data object Uploading : PhotoState
    data class Ready(val imageId: String, val previewUrl: String) : PhotoState
    data object Error : PhotoState
}

object SubmitError

sealed interface PostComposeEvent {
    data object SubmitSuccess : PostComposeEvent
}

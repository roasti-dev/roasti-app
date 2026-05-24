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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import dev.roasti.core.utils.imageUrl
import dev.roasti.feature.post.data.paging.PagingPostRepository
import dev.roasti.feature.upload.domain.UploadRepository

const val MAX_POST_PHOTOS = 10

@OptIn(ExperimentalUuidApi::class)
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

    fun onImagesPicked(picks: List<PickedImage>) {
        if (picks.isEmpty()) return
        val current = _state.value
        val remaining = MAX_POST_PHOTOS - current.photos.size
        val accepted = picks.take(remaining)
        if (accepted.size < picks.size) {
            eventsChannel.trySend(PostComposeEvent.MaxPhotosReached)
        }

        val newSlots = accepted.map { PhotoSlot.Uploading(localId = Uuid.random().toString(), fileName = it.fileName) }
        _state.update { it.copy(photos = it.photos + newSlots, submitError = null) }

        accepted.zip(newSlots).forEach { (pick, slot) -> uploadPhoto(slot.localId, pick) }
    }

    fun onRemovePhoto(localId: String) {
        _state.update { it.copy(photos = it.photos.filterNot { slot -> slot.localId == localId }) }
    }

    fun onSubmit() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(isSubmitting = true, submitError = null) }

        val title = current.title.trim()
        val body = current.body.trim().takeIf { it.isNotEmpty() }
        val imageIds = current.photos.filterIsInstance<PhotoSlot.Ready>().map { it.imageId }

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

    private fun uploadPhoto(localId: String, pick: PickedImage) {
        viewModelScope.launch {
            uploadRepository.uploadImage(pick.fileName, pick.bytes).fold(
                onSuccess = { uploaded ->
                    _state.update { it.copy(photos = it.photos.replaceById(localId) {
                        PhotoSlot.Ready(localId = localId, imageId = uploaded.id, previewUrl = imageUrl(uploaded.id))
                    }) }
                },
                onFailure = {
                    _state.update { it.copy(photos = it.photos.replaceById(localId) {
                        PhotoSlot.Error(localId = localId, fileName = pick.fileName)
                    }) }
                },
            )
        }
    }

    private fun loadExistingPost(id: String) {
        viewModelScope.launch {
            val post = pagingPostRepository.observePostById(id).first { it != null } ?: return@launch
            val photos = post.images.map { imageId ->
                PhotoSlot.Ready(
                    localId = Uuid.random().toString(),
                    imageId = imageId,
                    previewUrl = imageUrl(imageId),
                )
            }
            _state.update {
                it.copy(
                    title = post.title.orEmpty(),
                    body = post.text,
                    photos = photos,
                    isLoadingExisting = false,
                )
            }
        }
    }
}

private inline fun List<PhotoSlot>.replaceById(localId: String, transform: (PhotoSlot) -> PhotoSlot): List<PhotoSlot> =
    map { if (it.localId == localId) transform(it) else it }

enum class PostComposeMode { CREATE, EDIT }

data class PickedImage(val fileName: String, val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

data class PostComposeUiState(
    val mode: PostComposeMode = PostComposeMode.CREATE,
    val title: String = "",
    val body: String = "",
    val photos: List<PhotoSlot> = emptyList(),
    val isLoadingExisting: Boolean = false,
    val isSubmitting: Boolean = false,
    val submitError: SubmitError? = null,
) {
    val canSubmit: Boolean
        get() = !isSubmitting && !isLoadingExisting && title.isNotBlank() &&
            photos.none { it is PhotoSlot.Uploading || it is PhotoSlot.Error }

    val canAddMorePhotos: Boolean get() = photos.size < MAX_POST_PHOTOS
    val remainingPhotoSlots: Int get() = (MAX_POST_PHOTOS - photos.size).coerceAtLeast(0)
}

sealed interface PhotoSlot {
    val localId: String

    data class Uploading(override val localId: String, val fileName: String) : PhotoSlot
    data class Ready(override val localId: String, val imageId: String, val previewUrl: String) : PhotoSlot
    data class Error(override val localId: String, val fileName: String) : PhotoSlot
}

object SubmitError

sealed interface PostComposeEvent {
    data object SubmitSuccess : PostComposeEvent
    data object MaxPhotosReached : PostComposeEvent
}

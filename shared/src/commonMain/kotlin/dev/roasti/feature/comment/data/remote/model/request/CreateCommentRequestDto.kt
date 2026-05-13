package dev.roasti.feature.comment.data.remote.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateCommentRequestDto(
    @SerialName("text")
    val text: String,
    @SerialName("parent_id")
    val parentId: String? = null,
)

package dev.roasti.feature.comment.data.remote.model.response

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommentResponseDto(
    @SerialName("id")
    val id: String,
    @SerialName("is_deleted")
    val isDeleted: Boolean,
    @SerialName("author")
    val author: CommentAuthorDto? = null,
    @SerialName("text")
    val text: String,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant,
    @SerialName("parent_id")
    val parentId: String? = null,
)

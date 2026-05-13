package dev.roasti.features.comments

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import dev.roasti.common.api.ApiError

enum class CommentErrorCode {
    NOT_FOUND,
    FORBIDDEN,
    EMPTY_TEXT,
    TEXT_TOO_LONG,
    PARENT_NOT_FOUND
}

@Serializable
data class CommentError(
    @SerialName("code") override val code: CommentErrorCode,
    @SerialName("message") override val message: String,
) : ApiError

fun CommentErrorCode.toError() = when (this) {
    CommentErrorCode.NOT_FOUND -> CommentError(this, "Comment not found")
    CommentErrorCode.FORBIDDEN -> CommentError(this, "Forbidden")
    CommentErrorCode.EMPTY_TEXT -> CommentError(this, "Comment text must not be empty")
    CommentErrorCode.TEXT_TOO_LONG -> CommentError(this, "Comment text must be at most $TEXT_MAX_LENGTH characters")
    CommentErrorCode.PARENT_NOT_FOUND -> CommentError(this, "Parent comment not found")
}

fun CommentErrorCode.toHttpStatus() = when (this) {
    CommentErrorCode.NOT_FOUND -> HttpStatusCode.NotFound
    CommentErrorCode.FORBIDDEN -> HttpStatusCode.Forbidden
    CommentErrorCode.EMPTY_TEXT -> HttpStatusCode.UnprocessableEntity
    CommentErrorCode.TEXT_TOO_LONG -> HttpStatusCode.UnprocessableEntity
    CommentErrorCode.PARENT_NOT_FOUND -> HttpStatusCode.NotFound
}

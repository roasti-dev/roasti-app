package dev.roasti.features.posts

import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import dev.roasti.common.api.ApiError

enum class PostErrorCode { NOT_FOUND, FORBIDDEN, INVALID_INPUT }

@Serializable
data class PostError(
    @SerialName("code") override val code: PostErrorCode,
    @SerialName("message") override val message: String,
) : ApiError

fun PostErrorCode.toError() = when (this) {
    PostErrorCode.NOT_FOUND -> PostError(this, "Post not found")
    PostErrorCode.FORBIDDEN -> PostError(this, "Forbidden")
    PostErrorCode.INVALID_INPUT -> PostError(this, "Invalid input")
}

fun PostErrorCode.toHttpStatus() = when (this) {
    PostErrorCode.NOT_FOUND -> HttpStatusCode.NotFound
    PostErrorCode.FORBIDDEN -> HttpStatusCode.Forbidden
    PostErrorCode.INVALID_INPUT -> HttpStatusCode.UnprocessableEntity
}

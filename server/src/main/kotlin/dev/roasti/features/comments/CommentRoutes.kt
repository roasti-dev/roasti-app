package dev.roasti.features.comments

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import dev.roasti.FIREBASE_AUTH
import dev.roasti.FirebasePrincipal
import dev.roasti.common.api.ApiError
import dev.roasti.common.api.ApiErrorCode
import dev.roasti.common.api.respondError
import dev.roasti.feature.comment.data.remote.model.request.UpdateCommentRequestDto
import dev.roasti.feature.comment.data.remote.model.response.CommentAuthorDto
import dev.roasti.feature.comment.data.remote.model.response.CommentResponseDto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun Route.commentRoutes() {
    val commentService by inject<CommentService>()

    route("/comments/{id}") {
        authenticate(FIREBASE_AUTH) {
            patch {
                val id = call.parameters["id"]?.let { CommentId(Uuid.parse(it)) }
                    ?: return@patch call.respond(HttpStatusCode.BadRequest)
                val userId = call.principal<FirebasePrincipal>()!!.id
                val body = call.receive<UpdateCommentRequestDto>()
                commentService.update(userId, id, body.text).fold(
                    ifLeft = { call.respondError(it, UpdateCommentError::toHttp) },
                    ifRight = { call.respond(it.toDto()) },
                )
            }

            delete {
                val id = call.parameters["id"]?.let { CommentId(Uuid.parse(it)) }
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userId = call.principal<FirebasePrincipal>()!!.id
                commentService.delete(userId, id).fold(
                    ifLeft = { call.respond(HttpStatusCode.NoContent) },
                    ifRight = { call.respond(HttpStatusCode.NoContent) },
                )
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
internal fun Comment.toDto() = when (this) {
    is Comment.Active -> CommentResponseDto(
        id = id.value.toString(),
        isDeleted = false,
        author = CommentAuthorDto(
            author.id.value.toString(),
            author.username,
            author.name,
            author.avatarId
        ),
        text = text,
        parentId = parentId?.value?.toString(),
        createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(createdAt.toEpochMilliseconds()),
        updatedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(updatedAt.toEpochMilliseconds()),
    )

    is Comment.Deleted -> CommentResponseDto(
        id = id.value.toString(),
        isDeleted = true,
        author = null,
        text = "",
        parentId = parentId?.value?.toString(),
        createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(createdAt.toEpochMilliseconds()),
        updatedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(updatedAt.toEpochMilliseconds()),
    )
}

fun CreateCommentError.toHttp() = when (this) {
    is CreateCommentError.InvalidInput -> HttpStatusCode.UnprocessableEntity to ApiError(
        ApiErrorCode.INVALID_INPUT, message
    )
}

private fun UpdateCommentError.toHttp() = when (this) {
    UpdateCommentError.NotFound -> HttpStatusCode.NotFound to ApiError(
        ApiErrorCode.COMMENT_NOT_FOUND,
        "Comment not found"
    )

    UpdateCommentError.Forbidden -> HttpStatusCode.Forbidden to ApiError(
        ApiErrorCode.FORBIDDEN,
        "Forbidden"
    )

    is UpdateCommentError.InvalidInput -> HttpStatusCode.UnprocessableEntity to ApiError(
        ApiErrorCode.INVALID_INPUT, message
    )
}
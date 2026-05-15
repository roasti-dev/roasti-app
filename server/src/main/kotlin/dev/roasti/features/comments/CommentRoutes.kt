package dev.roasti.features.comments

import dev.roasti.FIREBASE_AUTH
import dev.roasti.FirebasePrincipal
import dev.roasti.common.api.ApiError
import dev.roasti.common.api.ApiErrorCode
import dev.roasti.common.api.respondError
import dev.roasti.feature.comment.data.remote.model.request.UpdateCommentRequestDto
import dev.roasti.feature.comment.data.remote.model.response.CommentAuthorDto
import dev.roasti.feature.comment.data.remote.model.response.CommentResponseDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.patch
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlin.uuid.ExperimentalUuidApi
import org.koin.ktor.ext.inject

@OptIn(ExperimentalUuidApi::class)
fun Route.commentRoutes() {
  val commentService by inject<CommentService>()

  authenticate(FIREBASE_AUTH) {
    patch<Comments.ById> { res ->
      val userId = call.principal<FirebasePrincipal>()!!.id
      val body = call.receive<UpdateCommentRequestDto>()
      commentService
          .update(userId, res.id, body.text)
          .fold(
              ifLeft = { call.respondError(it, UpdateCommentError::toHttp) },
              ifRight = { call.respond(it.toDto()) },
          )
    }

    delete<Comments.ById> { res ->
      val userId = call.principal<FirebasePrincipal>()!!.id
      commentService
          .delete(userId, res.id)
          .fold(
              ifLeft = { call.respond(HttpStatusCode.NoContent) },
              ifRight = { call.respond(HttpStatusCode.NoContent) },
          )
    }
  }
}

@OptIn(ExperimentalUuidApi::class)
internal fun Comment.toDto() =
    when (this) {
      is Comment.Active ->
          CommentResponseDto(
              id = id.value.toString(),
              isDeleted = false,
              author =
                  CommentAuthorDto(
                      author.id.value.toString(),
                      author.username,
                      author.name,
                      author.avatarId?.value.toString(),
                  ),
              text = text,
              parentId = parentId?.value?.toString(),
              createdAt =
                  kotlinx.datetime.Instant.fromEpochMilliseconds(createdAt.toEpochMilliseconds()),
              updatedAt =
                  kotlinx.datetime.Instant.fromEpochMilliseconds(updatedAt.toEpochMilliseconds()),
          )

      is Comment.Deleted ->
          CommentResponseDto(
              id = id.value.toString(),
              isDeleted = true,
              author = null,
              text = "",
              parentId = parentId?.value?.toString(),
              createdAt =
                  kotlinx.datetime.Instant.fromEpochMilliseconds(createdAt.toEpochMilliseconds()),
              updatedAt =
                  kotlinx.datetime.Instant.fromEpochMilliseconds(updatedAt.toEpochMilliseconds()),
          )
    }

fun CreateCommentError.toHttp() =
    when (this) {
      CreateCommentError.TargetNotFound ->
          HttpStatusCode.NotFound to ApiError(ApiErrorCode.NOT_FOUND, "Target not found")

      CreateCommentError.TargetNotVisible ->
          HttpStatusCode.Forbidden to ApiError(ApiErrorCode.FORBIDDEN, "Target not visible")

      CreateCommentError.CommentsDisabled ->
          HttpStatusCode.UnprocessableEntity to
              ApiError(ApiErrorCode.INVALID_INPUT, "Comments are disabled for this target")

      CreateCommentError.ParentNotFound ->
          HttpStatusCode.UnprocessableEntity to
              ApiError(ApiErrorCode.INVALID_INPUT, "Parent comment not found")

      is CreateCommentError.InvalidInput ->
          HttpStatusCode.UnprocessableEntity to ApiError(ApiErrorCode.INVALID_INPUT, message)
    }

private fun UpdateCommentError.toHttp() =
    when (this) {
      UpdateCommentError.NotFound ->
          HttpStatusCode.NotFound to ApiError(ApiErrorCode.COMMENT_NOT_FOUND, "Comment not found")

      UpdateCommentError.Forbidden ->
          HttpStatusCode.Forbidden to ApiError(ApiErrorCode.FORBIDDEN, "Forbidden")

      is UpdateCommentError.InvalidInput ->
          HttpStatusCode.UnprocessableEntity to ApiError(ApiErrorCode.INVALID_INPUT, message)
    }

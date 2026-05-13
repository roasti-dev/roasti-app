package dev.roasti.features.posts

import dev.roasti.FIREBASE_AUTH
import dev.roasti.FirebasePrincipal
import dev.roasti.common.api.ApiError
import dev.roasti.common.api.ApiErrorCode
import dev.roasti.common.api.respondError
import dev.roasti.common.domain.Page
import dev.roasti.common.domain.toId
import dev.roasti.core.network.PageResponseDto
import dev.roasti.core.network.PaginationResponseDto
import dev.roasti.feature.comment.data.remote.model.request.CreateCommentRequestDto
import dev.roasti.feature.comment.data.remote.model.response.CommentThreadResponseDto
import dev.roasti.feature.post.data.remote.model.VoteDirectionDto
import dev.roasti.feature.post.data.remote.model.request.CreatePostRequestDto
import dev.roasti.feature.post.data.remote.model.request.UpdatePostRequestDto
import dev.roasti.feature.post.data.remote.model.request.VoteRequestDto
import dev.roasti.feature.post.data.remote.model.response.PostAuthorDto
import dev.roasti.feature.post.data.remote.model.response.PostRecipeRefDto
import dev.roasti.feature.post.data.remote.model.response.PostRecipeStatusDto
import dev.roasti.feature.post.data.remote.model.response.PostResponseDto
import dev.roasti.feature.post.data.remote.model.response.PostVoteResponseDto
import dev.roasti.features.comments.CommentId
import dev.roasti.features.comments.CommentService
import dev.roasti.features.comments.CommentTargetType
import dev.roasti.features.comments.CommentThread
import dev.roasti.features.comments.toDto
import dev.roasti.features.comments.toHttp
import dev.roasti.features.recipes.RecipeId
import dev.roasti.features.users.model.UserId
import dev.roasti.features.users.model.UserPreview
import dev.roasti.features.votes.VoteDirection
import dev.roasti.features.votes.VoteInfo
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.datetime.Instant
import org.koin.ktor.ext.inject

@OptIn(ExperimentalUuidApi::class)
fun Route.postRoutes() {
  val postService by inject<PostService>()
  val commentService by inject<CommentService>()

  route("/posts") {
    authenticate(FIREBASE_AUTH) {
      get {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
        val authorId =
            call.queryParameters["author_id"]?.toId(::UserId)
                ?: return@get call.respond(HttpStatusCode.BadRequest)
        val userId = call.principal<FirebasePrincipal>()?.id
        val postsPage = postService.list(page, limit, authorId, userId)
        call.respond(postsPage.toDto())
      }

      get("/{id}") {
        val id =
            call.pathParameters["id"]?.toId(::PostId)
                ?: return@get call.respond(HttpStatusCode.BadRequest)
        val userId = call.principal<FirebasePrincipal>()?.id
        postService
            .getById(id, userId)
            .fold(
                ifLeft = { call.respondError(it, GetPostError::toHttp) },
                ifRight = { call.respond(it.toDto()) },
            )
      }

      get("/{id}/comments") {
        val id =
            call.pathParameters["id"]?.toId(::PostId)
                ?: return@get call.respond(HttpStatusCode.BadRequest)
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
        val result = commentService.list(id.value, CommentTargetType.POST, page, limit)
        call.respond(
            PageResponseDto(
                items = result.items.map { it.toDto() },
                pagination =
                    PaginationResponseDto(
                        currentPage = result.currentPage,
                        itemsCount = result.itemsCount,
                        lastPage = result.lastPage,
                        nextPage = result.nextPage,
                    ),
            )
        )
      }

      post {
        val userId =
            call.principal<FirebasePrincipal>()?.id
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val body = call.receive<CreatePostRequestDto>()
        val recipeId =
            body.recipeId?.let {
              it.toId(::RecipeId) ?: return@post call.respond(HttpStatusCode.BadRequest)
            }
        postService
            .create(
                userId,
                PostInput(
                    title = body.title,
                    text = body.text,
                    images = body.images,
                    recipeId = recipeId,
                ),
            )
            .fold(
                ifLeft = { call.respondError(it, CreatePostError::toHttp) },
                ifRight = { call.respond(HttpStatusCode.Created, it.toDto()) },
            )
      }

      put("/{id}") {
        val id =
            call.pathParameters["id"]?.toId(::PostId)
                ?: return@put call.respond(HttpStatusCode.BadRequest)
        val userId =
            call.principal<FirebasePrincipal>()?.id
                ?: return@put call.respond(HttpStatusCode.Unauthorized)
        val body = call.receive<UpdatePostRequestDto>()
        val recipeId =
            body.recipeId?.let {
              it.toId(::RecipeId) ?: return@put call.respond(HttpStatusCode.BadRequest)
            }
        postService
            .update(
                userId,
                id,
                PostInput(
                    title = body.title,
                    text = body.text,
                    images = body.images,
                    recipeId = recipeId,
                ),
            )
            .fold(
                ifLeft = { call.respondError(it, UpdatePostError::toHttp) },
                ifRight = { call.respond(it.toDto()) },
            )
      }

      delete("/{id}") {
        val id =
            call.pathParameters["id"]?.toId(::PostId)
                ?: return@delete call.respond(HttpStatusCode.BadRequest)
        val userId =
            call.principal<FirebasePrincipal>()?.id
                ?: return@delete call.respond(HttpStatusCode.Unauthorized)
        postService
            .delete(userId, id)
            .fold(
                ifLeft = { call.respond(HttpStatusCode.NoContent) },
                ifRight = { call.respond(HttpStatusCode.NoContent) },
            )
      }

      put("/{id}/vote") {
        val id =
            call.pathParameters["id"]?.toId(::PostId)
                ?: return@put call.respond(HttpStatusCode.BadRequest)
        val userId =
            call.principal<FirebasePrincipal>()?.id
                ?: return@put call.respond(HttpStatusCode.Unauthorized)
        val body = call.receive<VoteRequestDto>()
        val direction = body.type.toDomain()
        postService
            .vote(userId, id, direction)
            .fold(
                ifLeft = { call.respondError(it, VotePostError::toHttp) },
                ifRight = { call.respond(it.toDto()) },
            )
      }

      post("/{id}/comments") {
        val id =
            call.pathParameters["id"]?.toId(::PostId)
                ?: return@post call.respond(HttpStatusCode.BadRequest)
        val userId =
            call.principal<FirebasePrincipal>()?.id
                ?: return@post call.respond(HttpStatusCode.Unauthorized)
        val body = call.receive<CreateCommentRequestDto>()
        val parentId =
            body.parentId?.let {
              it.toId(::CommentId) ?: return@post call.respond(HttpStatusCode.BadRequest)
            }
        postService
            .createComment(userId, id, body.text, parentId)
            .fold(
                ifLeft = { call.respondError(it, CreatePostCommentError::toHttp) },
                ifRight = { call.respond(HttpStatusCode.Created, it.toDto()) },
            )
      }
    }
  }
}

private fun VoteInfo.toDto() = PostVoteResponseDto(rating = rating, userVote = userVote.toDto())

private fun VoteDirectionDto.toDomain() =
    when (this) {
      VoteDirectionDto.UP -> VoteDirection.UP
      VoteDirectionDto.DOWN -> VoteDirection.DOWN
      VoteDirectionDto.NONE -> VoteDirection.NONE
    }

private fun VoteDirection.toDto() =
    when (this) {
      VoteDirection.UP -> VoteDirectionDto.UP
      VoteDirection.DOWN -> VoteDirectionDto.DOWN
      VoteDirection.NONE -> VoteDirectionDto.NONE
    }

private fun UserPreview.toDto() =
    PostAuthorDto(id = id.value.toString(), username = username, name = name, avatarId = avatarId)

@OptIn(ExperimentalUuidApi::class)
private fun Post.toDto() =
    PostResponseDto(
        id = id.value.toString(),
        author = author.toDto(),
        title = title,
        text = text.orEmpty(),
        images = images,
        // TODO: check if recipe still exists and return UNAVAILABLE if deleted (requires batch
        // lookup in PostService)
        recipe =
            recipeId?.let {
              PostRecipeRefDto(id = it.toString(), status = PostRecipeStatusDto.AVAILABLE)
            },
        rating = rating,
        userVote = userVote.toDto(),
        commentsCount = commentsCount,
        createdAt = Instant.fromEpochMilliseconds(createdAt.toEpochMilliseconds()),
        updatedAt = Instant.fromEpochMilliseconds(updatedAt.toEpochMilliseconds()),
    )

private fun Page<Post>.toDto() =
    PageResponseDto(
        items = items.map { it.toDto() },
        pagination =
            PaginationResponseDto(
                currentPage = currentPage,
                itemsCount = itemsCount,
                lastPage = lastPage,
                nextPage = nextPage,
            ),
    )

private fun CommentThread.toDto() =
    root.toDto().let { r ->
      CommentThreadResponseDto(
          id = r.id,
          isDeleted = r.isDeleted,
          author = r.author,
          text = r.text,
          parentId = r.parentId,
          replies = replies.map { it.toDto() },
          createdAt = r.createdAt,
          updatedAt = r.updatedAt,
      )
    }

private fun GetPostError.toHttp() =
    when (this) {
      GetPostError.NotFound ->
          HttpStatusCode.NotFound to ApiError(ApiErrorCode.POST_NOT_FOUND, "Post not found")
    }

private fun CreatePostError.toHttp() =
    when (this) {
      is CreatePostError.ValidationError -> error.toHttp()
      CreatePostError.RecipeNotFound ->
          HttpStatusCode.NotFound to
              ApiError(ApiErrorCode.RECIPE_NOT_FOUND, "The attached recipe not found")

      is CreatePostError.ImagesNotUploaded ->
          HttpStatusCode.UnprocessableEntity to
              ApiError(
                  ApiErrorCode.INVALID_INPUT,
                  "The following images were not uploaded: ${this.ids.joinToString(", ")}",
              )
    }

private fun UpdatePostError.toHttp() =
    when (this) {
      UpdatePostError.Forbidden ->
          HttpStatusCode.Forbidden to ApiError(ApiErrorCode.FORBIDDEN, "Forbidden")

      UpdatePostError.NotFound ->
          HttpStatusCode.NotFound to ApiError(ApiErrorCode.POST_NOT_FOUND, "Post not found")

      is UpdatePostError.ValidationError -> error.toHttp()
    }

private fun VotePostError.toHttp() =
    when (this) {
      VotePostError.PostNotFound ->
          HttpStatusCode.NotFound to ApiError(ApiErrorCode.POST_NOT_FOUND, "Post not found")
    }

private fun CreatePostCommentError.toHttp() =
    when (this) {
      CreatePostCommentError.PostNotFound ->
          HttpStatusCode.NotFound to ApiError(ApiErrorCode.POST_NOT_FOUND, "Post not found")

      is CreatePostCommentError.CommentError -> error.toHttp()
    }

fun PostContentValidationError.toHttp() =
    when (this) {
      PostContentValidationError.TitleBlank ->
          HttpStatusCode.UnprocessableEntity to
              ApiError(ApiErrorCode.INVALID_INPUT, "The title cannot be empty")
      PostContentValidationError.NoContent ->
          HttpStatusCode.UnprocessableEntity to
              ApiError(
                  ApiErrorCode.INVALID_INPUT,
                  "The post must contain at least a text or images",
              )
    }

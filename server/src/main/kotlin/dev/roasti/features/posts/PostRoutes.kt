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
import dev.roasti.features.comments.CommentTarget
import dev.roasti.features.comments.CommentThread
import dev.roasti.features.comments.CreateCommentError
import dev.roasti.features.comments.toDto
import dev.roasti.features.comments.toHttp
import dev.roasti.features.recipes.RecipeId
import dev.roasti.features.uploads.ImageId
import dev.roasti.features.users.model.UserPreview
import dev.roasti.features.votes.VoteDirection
import dev.roasti.features.votes.VoteInfo
import dev.roasti.features.votes.VoteService
import dev.roasti.features.votes.VoteTarget
import dev.roasti.features.votes.VoteTargetError
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.resources.delete
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.datetime.Instant
import org.koin.ktor.ext.inject

@OptIn(ExperimentalUuidApi::class)
fun Route.postRoutes() {
  val postService by inject<PostService>()
  val commentService by inject<CommentService>()
  val voteService by inject<VoteService>()

  authenticate(FIREBASE_AUTH) {
    get<Posts> { res ->
      val userId = call.principal<FirebasePrincipal>()?.id
      val postsPage = postService.list(res.page, res.limit, res.authorId, userId)
      call.respond(postsPage.toDto())
    }

    get<Posts.ById> { res ->
      val userId = call.principal<FirebasePrincipal>()?.id
      postService
          .getById(res.id, userId)
          .fold(
              ifLeft = { call.respondError(it, GetPostError::toHttp) },
              ifRight = { call.respond(it.toDto()) },
          )
    }

    get<Posts.ById.Comments> { res ->
      val result = commentService.list(CommentTarget.Post(res.parent.id), res.page, res.limit)
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

    post<Posts> { _ ->
      val userId =
          call.principal<FirebasePrincipal>()?.id
              ?: return@post call.respond(HttpStatusCode.Unauthorized)
      val body = call.receive<CreatePostRequestDto>()
      val recipeId =
          body.recipeId?.let {
            it.toId(::RecipeId) ?: return@post call.respond(HttpStatusCode.BadRequest)
          }
      val images =
          body.images.map {
            it.toId(::ImageId) ?: return@post call.respond(HttpStatusCode.BadRequest)
          }
      postService
          .create(
              userId,
              PostInput(
                  title = body.title,
                  text = body.text,
                  images = images,
                  recipeId = recipeId,
              ),
          )
          .fold(
              ifLeft = { call.respondError(it, CreatePostError::toHttp) },
              ifRight = { call.respond(HttpStatusCode.Created, it.toDto()) },
          )
    }

    put<Posts.ById> { res ->
      val userId =
          call.principal<FirebasePrincipal>()?.id
              ?: return@put call.respond(HttpStatusCode.Unauthorized)
      val body = call.receive<UpdatePostRequestDto>()
      val recipeId =
          body.recipeId?.let {
            it.toId(::RecipeId) ?: return@put call.respond(HttpStatusCode.BadRequest)
          }
      val images =
          body.images.map {
            it.toId(::ImageId) ?: return@put call.respond(HttpStatusCode.BadRequest)
          }
      postService
          .update(
              userId,
              res.id,
              PostInput(
                  title = body.title,
                  text = body.text,
                  images = images,
                  recipeId = recipeId,
              ),
          )
          .fold(
              ifLeft = { call.respondError(it, UpdatePostError::toHttp) },
              ifRight = { call.respond(it.toDto()) },
          )
    }

    delete<Posts.ById> { res ->
      val userId =
          call.principal<FirebasePrincipal>()?.id
              ?: return@delete call.respond(HttpStatusCode.Unauthorized)
      postService
          .delete(userId, res.id)
          .fold(
              ifLeft = { call.respond(HttpStatusCode.NoContent) },
              ifRight = { call.respond(HttpStatusCode.NoContent) },
          )
    }

    put<Posts.ById.Vote> { res ->
      val userId =
          call.principal<FirebasePrincipal>()?.id
              ?: return@put call.respond(HttpStatusCode.Unauthorized)
      val body = call.receive<VoteRequestDto>()
      voteService
          .toggle(userId, VoteTarget.Post(res.parent.id), body.type.toDomain())
          .fold(
              ifLeft = { call.respondError(it, VoteTargetError::toHttp) },
              ifRight = { call.respond(it.toDto()) },
          )
    }

    post<Posts.ById.Comments> { res ->
      val userId =
          call.principal<FirebasePrincipal>()?.id
              ?: return@post call.respond(HttpStatusCode.Unauthorized)
      val body = call.receive<CreateCommentRequestDto>()
      val parentId =
          body.parentId?.let {
            it.toId(::CommentId) ?: return@post call.respond(HttpStatusCode.BadRequest)
          }
      commentService
          .create(userId, CommentTarget.Post(res.parent.id), body.text, parentId)
          .fold(
              ifLeft = { call.respondError(it, CreateCommentError::toHttp) },
              ifRight = { call.respond(HttpStatusCode.Created, it.toDto()) },
          )
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
    PostAuthorDto(
        id = id.value.toString(),
        username = username,
        name = name,
        avatarId = avatarId?.value.toString(),
    )

@OptIn(ExperimentalUuidApi::class)
private fun Post.toDto() =
    PostResponseDto(
        id = id.value.toString(),
        author = author.toDto(),
        title = title,
        text = text.orEmpty(),
        images = images.map { it.value.toString() },
        recipe =
            recipeRef?.let {
              PostRecipeRefDto(
                  id = it.id.value.toString(),
                  status =
                      when (it) {
                        is RecipeRef.Available -> PostRecipeStatusDto.AVAILABLE
                        is RecipeRef.Unavailable -> PostRecipeStatusDto.UNAVAILABLE
                      },
              )
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

      is UpdatePostError.ImagesNotUploaded ->
          HttpStatusCode.UnprocessableEntity to
              ApiError(
                  ApiErrorCode.INVALID_INPUT,
                  "The following images were not uploaded: ${this.ids.joinToString(", ")}",
              )
    }

private fun VoteTargetError.toHttp() =
    when (this) {
      VoteTargetError.NotFound ->
          HttpStatusCode.NotFound to ApiError(ApiErrorCode.POST_NOT_FOUND, "Post not found")
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

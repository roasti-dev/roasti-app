package dev.roasti.features.posts

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
import org.koin.ktor.ext.inject
import dev.roasti.FIREBASE_AUTH
import dev.roasti.FirebasePrincipal
import dev.roasti.common.api.respondError
import dev.roasti.feature.comment.data.remote.model.request.CreateCommentRequestDto
import dev.roasti.feature.comment.data.remote.model.response.CommentThreadResponseDto
import dev.roasti.core.network.PageResponseDto
import dev.roasti.core.network.PaginationResponseDto
import dev.roasti.common.domain.Page
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
import dev.roasti.features.votes.VoteDirection
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun Route.postRoutes() {
    val postService by inject<PostService>()
    val commentService by inject<CommentService>()

    route("/posts") {
        authenticate(FIREBASE_AUTH) {
            get {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val authorId = call.request.queryParameters["author_id"]
                    ?.let { dev.roasti.features.users.UserId(Uuid.parse(it)) }
                val userId = call.principal<FirebasePrincipal>()?.id
                val postsPage = postService.list(page, limit, authorId, userId)
                call.respond(postsPage.toDto())
            }

            get("/{id}") {
                val id = call.parameters["id"]?.let { PostId(Uuid.parse(it)) }
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val userId = call.principal<FirebasePrincipal>()?.id
                postService.getById(id, userId).fold(
                    ifLeft = { call.respondError(it.toHttpStatus(), it.toError()) },
                    ifRight = { call.respond(it.toDto()) },
                )
            }

            get("/{id}/comments") {
                val id = call.parameters["id"]?.let { Uuid.parse(it) }
                    ?: return@get call.respond(HttpStatusCode.BadRequest)
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20
                val result = commentService.list(id, CommentTargetType.POST, page, limit)
                call.respond(
                    PageResponseDto(
                        items = result.items.map { it.toDto() },
                        pagination = PaginationResponseDto(
                            currentPage = result.currentPage,
                            itemsCount = result.itemsCount,
                            lastPage = result.lastPage,
                            nextPage = result.nextPage,
                        ),
                    )
                )
            }

            post {
                val userId = call.principal<FirebasePrincipal>()!!.id
                val body = call.receive<CreatePostRequestDto>()
                postService.create(
                    userId,
                    CreatePostInput(
                        title = body.title,
                        text = body.text,
                        images = body.images,
                        recipeId = body.recipeId?.let { Uuid.parse(it) },
                    ),
                ).fold(
                    ifLeft = { call.respondError(it.toHttpStatus(), it.toError()) },
                    ifRight = { call.respond(HttpStatusCode.Created, it.toDto()) },
                )
            }

            put("/{id}") {
                val id = call.parameters["id"]?.let { PostId(Uuid.parse(it)) }
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userId = call.principal<FirebasePrincipal>()!!.id
                val body = call.receive<UpdatePostRequestDto>()
                postService.update(
                    userId, id,
                    UpdatePostInput(
                        title = body.title,
                        text = body.text,
                        images = body.images,
                        recipeId = body.recipeId?.let { Uuid.parse(it) },
                    ),
                ).fold(
                    ifLeft = { call.respondError(it.toHttpStatus(), it.toError()) },
                    ifRight = { call.respond(it.toDto()) },
                )
            }

            delete("/{id}") {
                val id = call.parameters["id"]?.let { PostId(Uuid.parse(it)) }
                    ?: return@delete call.respond(HttpStatusCode.BadRequest)
                val userId = call.principal<FirebasePrincipal>()!!.id
                postService.delete(userId, id).fold(
                    ifLeft = { call.respondError(it.toHttpStatus(), it.toError()) },
                    ifRight = { call.respond(HttpStatusCode.NoContent) },
                )
            }

            put("/{id}/vote") {
                val id = call.parameters["id"]?.let { PostId(Uuid.parse(it)) }
                    ?: return@put call.respond(HttpStatusCode.BadRequest)
                val userId = call.principal<FirebasePrincipal>()!!.id
                val body = call.receive<VoteRequestDto>()
                val direction = body.type.toDomain()
                postService.vote(userId, id, direction).fold(
                    ifLeft = { call.respondError(it.toHttpStatus(), it.toError()) },
                    ifRight = {
                        call.respond(
                            PostVoteResponseDto(
                                rating = it.rating,
                                userVote = it.userVote.toDto()
                            )
                        )
                    },
                )
            }

            post("/{id}/comments") {
                val id = call.parameters["id"]?.let { PostId(Uuid.parse(it)) }
                    ?: return@post call.respond(HttpStatusCode.BadRequest)
                val userId = call.principal<FirebasePrincipal>()!!.id
                val body = call.receive<CreateCommentRequestDto>()
                val parentId = body.parentId?.let { CommentId(Uuid.parse(it)) }
                postService.createComment(userId, id, body.text, parentId).fold(
                    ifLeft = { call.respondError(it.toHttpStatus(), it.toError()) },
                    ifRight = { call.respond(HttpStatusCode.Created, it.toDto()) },
                )
            }
        }
    }
}

private fun VoteDirectionDto.toDomain() = when (this) {
    VoteDirectionDto.UP -> VoteDirection.UP
    VoteDirectionDto.DOWN -> VoteDirection.DOWN
    VoteDirectionDto.NONE -> VoteDirection.NONE
}

private fun VoteDirection.toDto() = when (this) {
    VoteDirection.UP -> VoteDirectionDto.UP
    VoteDirection.DOWN -> VoteDirectionDto.DOWN
    VoteDirection.NONE -> VoteDirectionDto.NONE
}

@OptIn(ExperimentalUuidApi::class)
private fun Post.toDto() = PostResponseDto(
    id = id.value.toString(),
    author = PostAuthorDto(
        id = author.id.value.toString(),
        username = author.username,
        name = author.name,
        avatarId = author.avatarId,
    ),
    title = title,
    text = text ?: "",
    images = images,
    // TODO: check if recipe still exists and return UNAVAILABLE if deleted (requires batch lookup in PostService)
    recipe = recipeId?.let { PostRecipeRefDto(id = it.toString(), status = PostRecipeStatusDto.AVAILABLE) },
    rating = rating,
    userVote = userVote.toDto(),
    commentsCount = commentsCount,
    createdAt = kotlinx.datetime.Instant.fromEpochMilliseconds(createdAt.toEpochMilliseconds()),
    updatedAt = kotlinx.datetime.Instant.fromEpochMilliseconds(updatedAt.toEpochMilliseconds()),
)

private fun Page<Post>.toDto() = PageResponseDto(
    items = items.map { it.toDto() },
    pagination = PaginationResponseDto(
        currentPage = currentPage,
        itemsCount = itemsCount,
        lastPage = lastPage,
        nextPage = nextPage,
    ),
)


private fun CommentThread.toDto() = root.toDto().let { r ->
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

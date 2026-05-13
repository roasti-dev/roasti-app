package dev.roasti.features.posts

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.context.bind
import arrow.core.raise.either
import arrow.core.raise.ensure
import dev.roasti.common.domain.Page
import dev.roasti.features.comments.Comment
import dev.roasti.features.comments.CommentId
import dev.roasti.features.comments.CommentService
import dev.roasti.features.comments.CommentTargetType
import dev.roasti.features.comments.CreateCommentError
import dev.roasti.features.recipes.RecipeService
import dev.roasti.features.uploads.UploadService
import dev.roasti.features.users.model.UserId
import dev.roasti.features.votes.VoteDirection
import dev.roasti.features.votes.VoteInfo
import dev.roasti.features.votes.VoteService
import dev.roasti.features.votes.VoteTargetType
import kotlin.uuid.ExperimentalUuidApi

sealed interface GetPostError {
  data object NotFound : GetPostError
}

sealed interface PostContentValidationError {
  data object TitleBlank : PostContentValidationError

  data object NoContent : PostContentValidationError
}

sealed interface CreatePostError {
  data object RecipeNotFound : CreatePostError

  data class ValidationError(val error: PostContentValidationError) : CreatePostError

  data class ImagesNotUploaded(val ids: List<String>) : CreatePostError
}

sealed interface UpdatePostError {
  data object NotFound : UpdatePostError

  data object Forbidden : UpdatePostError

  data class ValidationError(val error: PostContentValidationError) : UpdatePostError
}

sealed interface DeletePostError {
  data object NotFound : DeletePostError

  data object Forbidden : DeletePostError
}

sealed interface VotePostError {
  data object PostNotFound : VotePostError
}

sealed interface CreatePostCommentError {
  data object PostNotFound : CreatePostCommentError

  data class CommentError(val error: CreateCommentError) : CreatePostCommentError
}

interface PostService {
  suspend fun getById(postId: PostId, userId: UserId?): Either<GetPostError, Post>

  suspend fun list(page: Int, limit: Int, authorId: UserId?, userId: UserId?): Page<Post>

  suspend fun create(userId: UserId, input: PostInput): Either<CreatePostError, Post>

  suspend fun update(
      userId: UserId,
      postId: PostId,
      input: PostInput,
  ): Either<UpdatePostError, Post>

  suspend fun delete(userId: UserId, postId: PostId): Either<DeletePostError, Unit>

  suspend fun vote(
      userId: UserId,
      postId: PostId,
      direction: VoteDirection,
  ): Either<VotePostError, VoteInfo>

  suspend fun createComment(
      userId: UserId,
      postId: PostId,
      text: String,
      parentId: CommentId?,
  ): Either<CreatePostCommentError, Comment>
}

@OptIn(ExperimentalUuidApi::class)
class PostServiceImpl(
    private val repo: PostRepository,
    private val recipeService: RecipeService,
    private val voteService: VoteService,
    private val commentService: CommentService,
    private val uploadsService: UploadService,
) : PostService {

  override suspend fun getById(postId: PostId, userId: UserId?): Either<GetPostError, Post> =
      either {
        val row = repo.findById(postId) ?: raise(GetPostError.NotFound)
        row.enrich(userId)
      }

  override suspend fun list(
      page: Int,
      limit: Int,
      authorId: UserId?,
      userId: UserId?,
  ): Page<Post> {
    val (rows, total) = repo.list(page, limit, authorId)
    val postIds = rows.map { it.id.value }
    val voteInfos = voteService.getInfoBatch(userId, postIds, VoteTargetType.POST)
    val commentCounts = commentService.countForTargetBatch(postIds, CommentTargetType.POST)

    val posts =
        rows.map { row ->
          val id = row.id.value
          row.toPost(
              voteInfo = voteInfos.getValue(id),
              commentsCount = commentCounts.getValue(id),
          )
        }

    return Page.of(posts, page, total, limit)
  }

  override suspend fun create(userId: UserId, input: PostInput): Either<CreatePostError, Post> =
      either {
        input.validate().mapLeft { CreatePostError.ValidationError(it) }.bind()

        input.recipeId?.let { recipeId ->
          recipeService.getById(recipeId, userId).getOrElse {
            raise(CreatePostError.RecipeNotFound)
          }
        }

        //        val missing = input.images.filter { uploadsService.findById(it) == null }
        //        if (missing.isNotEmpty()) raise(CreatePostError.ImagesNotUploaded(missing))

        val row = repo.create(userId, input)
        row.enrich(userId)
      }

  private fun PostInput.validate(): Either<PostContentValidationError, Unit> = either {
    title?.let { ensure(it.isNotBlank()) { PostContentValidationError.TitleBlank } }

    if (text.isNullOrBlank() && images.isEmpty()) raise(PostContentValidationError.NoContent)
  }

  override suspend fun update(
      userId: UserId,
      postId: PostId,
      input: PostInput,
  ): Either<UpdatePostError, Post> = either {
    input.validate().mapLeft { UpdatePostError.ValidationError(it) }.bind()

    val existing = repo.findById(postId) ?: raise(UpdatePostError.NotFound)
    if (existing.author.id != userId) raise(UpdatePostError.Forbidden)
    val row = repo.update(postId, input) ?: raise(UpdatePostError.NotFound)
    row.enrich(userId)
  }

  override suspend fun delete(userId: UserId, postId: PostId): Either<DeletePostError, Unit> =
      either {
        val post = repo.findById(postId) ?: raise(DeletePostError.NotFound)
        if (post.author.id != userId) raise(DeletePostError.Forbidden)
        repo.delete(postId)
      }

  override suspend fun vote(
      userId: UserId,
      postId: PostId,
      direction: VoteDirection,
  ): Either<VotePostError, VoteInfo> = either {
    repo.findById(postId) ?: raise(VotePostError.PostNotFound)
    voteService.toggle(userId, postId.value, VoteTargetType.POST, direction)
  }

  override suspend fun createComment(
      userId: UserId,
      postId: PostId,
      text: String,
      parentId: CommentId?,
  ): Either<CreatePostCommentError, Comment> = either {
    repo.findById(postId) ?: raise(CreatePostCommentError.PostNotFound)
    commentService
        .create(userId, postId.value, CommentTargetType.POST, text, parentId)
        .mapLeft { CreatePostCommentError.CommentError(it) }
        .bind()
  }

  private suspend fun PostRow.enrich(userId: UserId?): Post {
    val voteInfo = voteService.getInfo(userId, id.value, VoteTargetType.POST)
    val commentsCount = commentService.countForTarget(id.value, CommentTargetType.POST)
    return toPost(voteInfo, commentsCount)
  }
}

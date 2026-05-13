package dev.roasti.features.posts

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import dev.roasti.features.comments.CommentRepository
import dev.roasti.features.comments.CommentTargetType
import dev.roasti.common.domain.Page
import dev.roasti.features.comments.Comment
import dev.roasti.features.comments.CommentId
import dev.roasti.features.comments.CommentService
import dev.roasti.features.recipes.RecipeErrorCode
import dev.roasti.features.recipes.RecipeId
import dev.roasti.features.users.UserId
import dev.roasti.features.votes.VoteDirection
import dev.roasti.features.votes.VoteService
import dev.roasti.features.votes.VoteTargetType
import kotlin.uuid.ExperimentalUuidApi

data class PostVoteResult(val rating: Int, val userVote: VoteDirection)

interface PostService {
    suspend fun getById(id: PostId, userId: UserId?): Either<PostErrorCode, Post>
    suspend fun list(page: Int, limit: Int, authorId: UserId?, userId: UserId?): Page<Post>
    suspend fun create(userId: UserId, input: CreatePostInput): Either<PostErrorCode, Post>
    suspend fun update(userId: UserId, id: PostId, input: UpdatePostInput): Either<PostErrorCode, Post>
    suspend fun delete(userId: UserId, id: PostId): Either<PostErrorCode, Unit>
    suspend fun vote(userId: UserId, id: PostId, direction: VoteDirection): Either<PostErrorCode, PostVoteResult>
    suspend fun createComment(userId: UserId, id: PostId, text: String, parentId: CommentId?): Either<PostErrorCode, Comment>
}

@OptIn(ExperimentalUuidApi::class)
class PostServiceImpl(
    private val repo: PostRepository,
    private val voteService: VoteService,
    private val commentService: CommentService,
) : PostService {

    override suspend fun getById(id: PostId, userId: UserId?): Either<PostErrorCode, Post> {
        val row = repo.findById(id) ?: return PostErrorCode.NOT_FOUND.left()
        return row.enrich(userId).right()
    }

    override suspend fun list(page: Int, limit: Int, authorId: UserId?, userId: UserId?): Page<Post> {
        val (rows, total) = repo.list(page, limit, authorId)
        val postIds = rows.map { it.id.value }
        val voteInfos = voteService.getInfoBatch(userId, postIds, VoteTargetType.POST)
        val commentCounts = commentService.countForTargetBatch(postIds, CommentTargetType.POST)

        val posts = rows.map { row ->
            val id = row.id.value
            val voteInfo = voteInfos.getValue(id)
            row.toPost(
                rating = voteInfo.rating,
                userVote = voteInfo.userVote,
                commentsCount = commentCounts.getValue(id),
            )
        }

        return Page.of(posts, page, total, limit)
    }

    override suspend fun create(userId: UserId, input: CreatePostInput): Either<PostErrorCode, Post> {
        if (input.title.isNullOrBlank() && input.text.isNullOrBlank() && input.images.isEmpty()) {
            return PostErrorCode.INVALID_INPUT.left()
        }
        val row = repo.create(userId, input)
        return row.enrich(userId).right()
    }

    override suspend fun update(userId: UserId, id: PostId, input: UpdatePostInput): Either<PostErrorCode, Post> {
        val existing = repo.findById(id) ?: return PostErrorCode.NOT_FOUND.left()
        if (existing.author.id != userId) return PostErrorCode.FORBIDDEN.left()
        val row = repo.update(id, input) ?: return PostErrorCode.NOT_FOUND.left()
        return row.enrich(userId).right()
    }

    override suspend fun delete(userId: UserId, id: PostId): Either<PostErrorCode, Unit> {
        val existing = repo.findById(id) ?: return PostErrorCode.NOT_FOUND.left()
        if (existing.author.id != userId) return PostErrorCode.FORBIDDEN.left()
        repo.delete(id)
        return Unit.right()
    }

    override suspend fun vote(userId: UserId, id: PostId, direction: VoteDirection): Either<PostErrorCode, PostVoteResult> {
        repo.findById(id) ?: return PostErrorCode.NOT_FOUND.left()
        val voteInfo = when (direction) {
            VoteDirection.UP, VoteDirection.DOWN -> voteService.put(userId, id.value, VoteTargetType.POST, direction)
            VoteDirection.NONE -> voteService.remove(userId, id.value, VoteTargetType.POST)
        }
        return PostVoteResult(voteInfo.rating, voteInfo.userVote).right()
    }

    override suspend fun createComment(
        userId: UserId,
        id: PostId,
        text: String,
        parentId: CommentId?
    ): Either<PostErrorCode, Comment> {
        repo.findById(id) ?: return PostErrorCode.NOT_FOUND.left()
        return commentService.create(userId, id.value, CommentTargetType.POST, text, parentId).right()
    }

    private suspend fun PostRow.enrich(userId: UserId?): Post {
        val voteInfo = voteService.getInfo(userId, id.value, VoteTargetType.POST)
        val commentsCount = commentService.countForTarget(id.value, CommentTargetType.POST)
        return toPost(voteInfo.rating, voteInfo.userVote, commentsCount)
    }
}

@OptIn(ExperimentalUuidApi::class)
internal fun PostRow.toPost(rating: Int, userVote: VoteDirection, commentsCount: Int) = Post(
    id = id,
    author = author,
    title = title,
    text = text,
    images = images,
    recipeId = recipeId,
    rating = rating,
    userVote = userVote,
    commentsCount = commentsCount,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

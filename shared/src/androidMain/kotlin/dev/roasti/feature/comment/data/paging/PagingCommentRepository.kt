package dev.roasti.feature.comment.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.comment.data.mapper.toDomain
import dev.roasti.feature.comment.data.network.CommentsApiClient
import dev.roasti.feature.comment.data.remote.model.request.CreateCommentRequestDto
import dev.roasti.feature.comment.data.remote.model.request.UpdateCommentRequestDto
import dev.roasti.feature.comment.domain.model.Comment
import dev.roasti.feature.comment.domain.model.CommentThread

private const val CommentsPageSize = 20
private const val PrefetchDistance = 5

@OptIn(ExperimentalPagingApi::class)
class PagingCommentRepository(
    private val db: RoastiDatabaseCache,
    private val commentsApiClient: CommentsApiClient,
) {

    fun observeHasCachedComments(postId: String): Flow<Boolean> =
        db.commentEntityQueries.countRootByPostId(postId)
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map { count -> count > 0L }

    fun threadsPager(postId: String): Flow<PagingData<CommentThread>> = Pager(
        config = pagingConfig(),
        remoteMediator = CommentsRemoteMediator(
            postId = postId,
            pageSize = CommentsPageSize,
            api = commentsApiClient,
            db = db,
        ),
        pagingSourceFactory = {
            QueryPagingSource(
                countQuery = db.commentEntityQueries.countRootByPostId(postId),
                transacter = db.commentEntityQueries,
                context = Dispatchers.IO,
                queryProvider = { limit, offset ->
                    db.commentEntityQueries.selectRootByPostIdPaged(postId, limit, offset)
                },
            )
        },
    ).flow.map { pagingData ->
        pagingData.map { row ->
            val replies = db.commentEntityQueries
                .selectRepliesByParentId(row.id)
                .executeAsList()
                .map { it.toDomain() }
            CommentThread(root = row.toDomain(), replies = replies)
        }
    }

    suspend fun createComment(
        postId: String,
        text: String,
        parentId: String? = null,
    ): Result<Comment> = commentsApiClient
        .createComment(postId, CreateCommentRequestDto(text = text, parentId = parentId))
        .map { dto ->
            db.transaction {
                val maxPosition = if (dto.parentId == null) {
                    db.commentEntityQueries.selectMaxPositionForRoot(postId)
                        .executeAsOneOrNull()?.max_position
                } else {
                    db.commentEntityQueries.selectMaxPositionForReplies(dto.parentId)
                        .executeAsOneOrNull()?.max_position
                } ?: 0L
                db.commentEntityQueries.upsertComment(
                    id = dto.id,
                    post_id = postId,
                    parent_id = dto.parentId,
                    is_deleted = if (dto.isDeleted) 1L else 0L,
                    author_id = dto.author?.id,
                    author_username = dto.author?.username,
                    author_avatar_id = dto.author?.avatarId,
                    text = dto.text,
                    created_at = dto.createdAt.toString(),
                    updated_at = dto.updatedAt.toString(),
                    position = maxPosition + 1L,
                )
            }
            dto.toDomain()
        }

    suspend fun updateComment(
        commentId: String,
        text: String,
    ): Result<Comment> = commentsApiClient
        .updateComment(commentId, UpdateCommentRequestDto(text = text))
        .map { dto ->
            db.transaction {
                db.commentEntityQueries.updateCommentContent(
                    text = dto.text,
                    updated_at = dto.updatedAt.toString(),
                    is_deleted = if (dto.isDeleted) 1L else 0L,
                    author_id = dto.author?.id,
                    author_username = dto.author?.username,
                    author_avatar_id = dto.author?.avatarId,
                    id = dto.id,
                )
            }
            dto.toDomain()
        }

    suspend fun deleteComment(commentId: String): Result<Unit> =
        commentsApiClient.deleteComment(commentId).onSuccess {
            db.transaction {
                db.commentEntityQueries.softDeleteComment(
                    updated_at = Clock.System.now().toString(),
                    id = commentId,
                )
            }
        }

    private fun pagingConfig() = PagingConfig(
        pageSize = CommentsPageSize,
        prefetchDistance = PrefetchDistance,
        initialLoadSize = CommentsPageSize,
    )
}

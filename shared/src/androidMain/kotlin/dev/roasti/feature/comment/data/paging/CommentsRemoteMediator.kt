package dev.roasti.feature.comment.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import dev.roasti.CommentEntity
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.comment.data.mapper.upsertCommentThread
import dev.roasti.feature.comment.data.network.CommentsApiClient

@OptIn(ExperimentalPagingApi::class)
class CommentsRemoteMediator(
    private val postId: String,
    private val pageSize: Int,
    private val api: CommentsApiClient,
    private val db: RoastiDatabaseCache,
) : RemoteMediator<Int, CommentEntity>() {

    override suspend fun initialize(): InitializeAction =
        InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CommentEntity>,
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val remoteKey = db.commentRemoteKeyQueries
                    .getRemoteKey(postId)
                    .executeAsOneOrNull()
                remoteKey?.next_page?.toInt()
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        return try {
            val response = api.listComments(
                postId = postId,
                page = page,
                limit = state.config.pageSize.coerceAtLeast(pageSize),
            ).getOrThrow()

            val threads = response.items
            val pagination = response.pagination
            val endReached = pagination.currentPage >= pagination.lastPage

            db.transaction {
                if (loadType == LoadType.REFRESH) {
                    db.commentEntityQueries.deleteByPostId(postId)
                    db.commentRemoteKeyQueries.deleteByPostId(postId)
                }
                val pageOffset = (pagination.currentPage - 1) * state.config.pageSize
                threads.forEachIndexed { index, thread ->
                    db.upsertCommentThread(
                        postId = postId,
                        thread = thread,
                        rootPosition = pageOffset + index,
                    )
                }
                db.commentRemoteKeyQueries.upsertRemoteKey(
                    post_id = postId,
                    next_page = if (endReached) null else pagination.nextPage.toLong(),
                )
            }

            MediatorResult.Success(endOfPaginationReached = endReached)
        } catch (th: Throwable) {
            MediatorResult.Error(th)
        }
    }
}

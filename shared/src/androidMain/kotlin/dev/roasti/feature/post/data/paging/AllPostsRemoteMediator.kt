package dev.roasti.feature.post.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import dev.roasti.Post
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.post.data.mapper.upsertPost
import dev.roasti.feature.post.data.network.PostsApiClient

private const val AllPostsRemoteKeyId = "all_posts"

@OptIn(ExperimentalPagingApi::class)
class AllPostsRemoteMediator(
    private val postsApiClient: PostsApiClient,
    private val db: RoastiDatabaseCache,
) : RemoteMediator<Int, Post>() {

    override suspend fun initialize(): InitializeAction =
        InitializeAction.LAUNCH_INITIAL_REFRESH

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Post>,
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val remoteKey = db.postRemoteKeyQueries
                    .getRemoteKey(AllPostsRemoteKeyId)
                    .executeAsOneOrNull()
                remoteKey?.next_page?.toInt()
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        return try {
            val response = postsApiClient.getPosts(
                page = page,
                limit = state.config.pageSize,
            ).getOrThrow()

            val posts = response.items
            val pagination = response.pagination
            val endReached = pagination.currentPage >= pagination.lastPage

            db.transaction {
                if (loadType == LoadType.REFRESH) {
                    db.postQueries.clearAllPosts()
                    db.postRemoteKeyQueries.clearRemoteKeys(AllPostsRemoteKeyId)
                }

                posts.forEach { dto -> db.upsertPost(dto) }

                db.postRemoteKeyQueries.insertRemoteKey(
                    id = AllPostsRemoteKeyId,
                    next_page = if (endReached) null else pagination.nextPage.toLong(),
                )
            }

            MediatorResult.Success(endOfPaginationReached = endReached)
        } catch (th: Throwable) {
            MediatorResult.Error(th)
        }
    }
}

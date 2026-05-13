package dev.roasti.feature.post.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.paging3.QueryPagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import dev.roasti.Post as CachedPost
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.post.data.mapper.toDomain
import dev.roasti.feature.post.data.mapper.toDto
import dev.roasti.feature.post.data.mapper.toVoteDirection
import dev.roasti.feature.post.data.mapper.toWireString
import dev.roasti.feature.post.data.mapper.upsertPost
import dev.roasti.feature.post.data.network.PostsApiClient
import dev.roasti.feature.post.data.remote.model.request.CreatePostRequestDto
import dev.roasti.feature.post.data.remote.model.request.UpdatePostRequestDto
import dev.roasti.feature.post.data.remote.model.request.VoteRequestDto
import dev.roasti.feature.post.domain.model.Post
import dev.roasti.feature.post.domain.model.VoteDirection

private const val PostsPageSize = 20
private const val PrefetchDistance = 5

@OptIn(ExperimentalPagingApi::class)
class PagingPostRepository(
    private val db: RoastiDatabaseCache,
    private val postsApiClient: PostsApiClient,
    private val allPostsRemoteMediator: AllPostsRemoteMediator,
) {
    fun observePostById(id: String): Flow<Post?> =
        db.postQueries.getPostById(id)
            .asFlow()
            .map { query -> query.executeAsOneOrNull()?.toDomain() }

    fun observeHasCachedPosts(): Flow<Boolean> =
        db.postQueries.countAllPosts()
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map { count -> count > 0L }
            .distinctUntilChanged()

    suspend fun refreshPostById(id: String): Result<Unit> =
        postsApiClient.getPost(id).map { dto ->
            db.transaction { db.upsertPost(dto) }
        }

    fun getOfflineFirstPostsPager(): Flow<PagingData<CachedPost>> = Pager(
        config = pagingConfig(),
        remoteMediator = allPostsRemoteMediator,
        pagingSourceFactory = {
            QueryPagingSource(
                countQuery = db.postQueries.countAllPosts(),
                transacter = db.postQueries,
                context = Dispatchers.IO,
                queryProvider = { limit, offset ->
                    db.postQueries.getAllPosts(limit, offset)
                },
            )
        },
    ).flow

    /**
     * Optimistic update: mutates the local Post row immediately, calls the API,
     * and reconciles to the server response on success or rolls back on failure.
     */
    suspend fun setVote(postId: String, target: VoteDirection) {
        val current = db.postQueries.getPostById(postId).executeAsOneOrNull() ?: return
        val previousVote = current.user_vote.toVoteDirection()
        if (previousVote == target) return

        val previousRating = current.rating
        val previousVoteWire = current.user_vote
        val optimisticRating = current.rating + previousVote.deltaTo(target)
        val optimisticVoteWire = target.toWireString()

        db.transaction {
            db.postQueries.applyVote(
                rating = optimisticRating,
                user_vote = optimisticVoteWire,
                id = postId,
            )
        }

        postsApiClient.vote(postId, VoteRequestDto(target.toDto())).fold(
            onSuccess = { dto ->
                db.transaction {
                    db.postQueries.applyVote(
                        rating = dto.rating.toLong(),
                        user_vote = dto.userVote.toDomain().toWireString(),
                        id = postId,
                    )
                }
            },
            onFailure = {
                db.transaction {
                    db.postQueries.applyVote(
                        rating = previousRating,
                        user_vote = previousVoteWire,
                        id = postId,
                    )
                }
            },
        )
    }

    suspend fun createPost(title: String, text: String?, imageIds: List<String>): Result<Post> {
        val request = CreatePostRequestDto(
            title = title,
            text = text,
            images = imageIds,
            recipeId = null,
        )
        return postsApiClient.createPost(request).map { dto ->
            db.transaction { db.upsertPost(dto) }
            dto.toDomain()
        }
    }

    suspend fun updatePost(
        id: String,
        title: String,
        text: String?,
        imageIds: List<String>,
    ): Result<Post> {
        val request = UpdatePostRequestDto(
            title = title,
            text = text,
            images = imageIds,
            recipeId = null,
        )
        return postsApiClient.updatePost(id, request).map { dto ->
            db.transaction { db.upsertPost(dto) }
            dto.toDomain()
        }
    }

    suspend fun deletePost(id: String): Result<Unit> =
        postsApiClient.deletePost(id).onSuccess {
            db.transaction {
                db.postQueries.deletePost(id)
                db.commentEntityQueries.deleteByPostId(id)
                db.commentRemoteKeyQueries.deleteByPostId(id)
            }
        }

    private fun pagingConfig() = PagingConfig(
        pageSize = PostsPageSize,
        prefetchDistance = PrefetchDistance,
        initialLoadSize = PostsPageSize,
    )
}

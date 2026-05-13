package dev.roasti.feature.recipe.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import kotlinx.coroutines.flow.firstOrNull
import dev.roasti.Recipe
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.auth.domain.repository.AuthRepository
import dev.roasti.feature.likes.data.LikesApiClient
import dev.roasti.feature.recipe.data.RecipeListType
import dev.roasti.feature.recipe.data.mapper.upsertRecipe

@OptIn(ExperimentalPagingApi::class)
class FavoritesRemoteMediator(
    private val likesApiClient: LikesApiClient,
    private val authRepository: AuthRepository,
    private val db: RoastiDatabaseCache,
) : RemoteMediator<Int, Recipe>() {

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Recipe>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val remoteKey = db.recipeRemoteKeyQueries
                    .getRemoteKey(RecipeListType.FAVORITES)
                    .executeAsOneOrNull()
                remoteKey?.next_page?.toInt()
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        return try {
            val userId = authRepository.getUser().firstOrNull()?.id
                ?: return MediatorResult.Error(Exception("User not found"))

            val response = likesApiClient.getLikedRecipes(
                userId = userId,
                page = page,
                limit = state.config.pageSize,
            ).getOrThrow()

            val pagination = response.pagination
            val endReached = pagination.currentPage >= pagination.lastPage
            val basePosition = (page - 1L) * state.config.pageSize

            db.transaction {
                if (loadType == LoadType.REFRESH) {
                    db.recipeListMembershipQueries.clearList(RecipeListType.FAVORITES)
                    db.recipeRemoteKeyQueries.clearRemoteKeys(RecipeListType.FAVORITES)
                }

                response.items.forEachIndexed { index, item ->
                    db.upsertRecipe(item.recipe)
                    db.recipeListMembershipQueries.insertMembership(
                        listType = RecipeListType.FAVORITES,
                        recipeId = item.recipe.id,
                        position = basePosition + index,
                    )
                }

                db.recipeRemoteKeyQueries.insertRemoteKey(
                    id = RecipeListType.FAVORITES,
                    next_page = if (endReached) null else pagination.nextPage.toLong(),
                )
            }

            MediatorResult.Success(endOfPaginationReached = endReached)
        } catch (e: Exception) {
            MediatorResult.Error(e)
        }
    }
}

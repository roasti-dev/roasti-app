package dev.roasti.feature.recipe.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import kotlinx.coroutines.flow.first
import dev.roasti.Recipe
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.auth.domain.repository.AuthRepository
import dev.roasti.feature.recipe.data.RecipeListType
import dev.roasti.feature.recipe.data.network.RecipesApiClient
import dev.roasti.feature.recipe.data.mapper.upsertRecipe

@OptIn(ExperimentalPagingApi::class)
class AllRecipesRemoteMediator(
    private val authRepository: AuthRepository,
    private val recipesApiClient: RecipesApiClient,
    private val db: RoastiDatabaseCache,
) : RemoteMediator<Int, Recipe>() {

    private var userId: String? = null

    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(loadType: LoadType, state: PagingState<Int, Recipe>): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val remoteKey = db.recipeRemoteKeyQueries.getRemoteKey(RecipeListType.FEED)
                    .executeAsOneOrNull()
                remoteKey?.next_page?.toInt()
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
            }
        }

        return try {
            val response = recipesApiClient.getRecipes(
                authorId = assignCurrentUser(),
                page = page,
                limit = state.config.pageSize,
            ).getOrThrow()

            val recipes = response.items
            val pagination = response.pagination
            val endReached = pagination.currentPage >= pagination.lastPage
            val basePosition = (page - 1L) * state.config.pageSize

            db.transaction {
                if (loadType == LoadType.REFRESH) {
                    db.recipeListMembershipQueries.clearList(RecipeListType.FEED)
                    db.recipeRemoteKeyQueries.clearRemoteKeys(RecipeListType.FEED)
                }

                recipes.forEachIndexed { index, dto ->
                    db.upsertRecipe(dto)
                    db.recipeListMembershipQueries.insertMembership(
                        listType = RecipeListType.FEED,
                        recipeId = dto.id,
                        position = basePosition + index,
                    )
                }

                db.recipeRemoteKeyQueries.insertRemoteKey(
                    RecipeListType.FEED,
                    if (endReached) null else pagination.nextPage.toLong()
                )
            }

            MediatorResult.Success(endOfPaginationReached = endReached)
        } catch (th: Throwable) {
            MediatorResult.Error(th)
        }
    }

    private suspend fun assignCurrentUser(): String? {
        if (userId == null) {
            userId = authRepository.getUser().first()?.id
        }
        return userId
    }
}

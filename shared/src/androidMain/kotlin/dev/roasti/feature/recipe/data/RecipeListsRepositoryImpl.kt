package dev.roasti.feature.recipe.data

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
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.auth.domain.repository.AuthRepository
import dev.roasti.feature.recipe.data.mapper.toDomain
import dev.roasti.feature.recipe.data.network.RecipesApiClient
import dev.roasti.feature.recipe.data.paging.AllRecipesRemoteMediator
import dev.roasti.feature.recipe.data.paging.FavoritesRemoteMediator
import dev.roasti.feature.recipe.data.paging.RemoteRecipesPagingSource
import dev.roasti.feature.recipe.domain.RecipeListsRepository
import dev.roasti.feature.recipe.domain.model.Recipe
import dev.roasti.feature.recipe.domain.model.RecipesPagingQuery

private const val RecipesPageSize = 20

@OptIn(ExperimentalPagingApi::class)
class RecipeListsRepositoryImpl(
    private val db: RoastiDatabaseCache,
    private val recipesApiClient: RecipesApiClient,
    private val authRepository: AuthRepository,
    private val favoritesRemoteMediator: FavoritesRemoteMediator,
) : RecipeListsRepository {

    override fun observeHasCachedFeed(): Flow<Boolean> =
        db.recipeListMembershipQueries.countRecipesByList(RecipeListType.FEED)
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map { count -> count > 0L }

    override fun observeFeed(): Flow<PagingData<Recipe>> = Pager(
        config = createPagingConfig(),
        remoteMediator = AllRecipesRemoteMediator(
            authRepository = authRepository,
            recipesApiClient = recipesApiClient,
            db = db,
        ),
        pagingSourceFactory = {
            QueryPagingSource(
                countQuery = db.recipeListMembershipQueries.countRecipesByList(RecipeListType.FEED),
                transacter = db.recipeListMembershipQueries,
                context = Dispatchers.IO,
                queryProvider = { limit, offset ->
                    db.recipeListMembershipQueries.getRecipesByList(
                        listType = RecipeListType.FEED,
                        limit = limit,
                        offset = offset,
                    )
                }
            )
        }
    ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override fun observeFavorites(): Flow<PagingData<Recipe>> = Pager(
        config = createPagingConfig(),
        remoteMediator = favoritesRemoteMediator,
        pagingSourceFactory = {
            QueryPagingSource(
                countQuery = db.recipeListMembershipQueries.countRecipesByList(RecipeListType.FAVORITES),
                transacter = db.recipeListMembershipQueries,
                context = Dispatchers.IO,
                queryProvider = { limit, offset ->
                    db.recipeListMembershipQueries.getRecipesByList(
                        listType = RecipeListType.FAVORITES,
                        limit = limit,
                        offset = offset,
                    )
                }
            )
        }
    ).flow.map { pagingData -> pagingData.map { it.toDomain() } }

    override fun observeSearch(query: RecipesPagingQuery): Flow<PagingData<Recipe>> = Pager(
        config = createPagingConfig(),
        pagingSourceFactory = {
            RemoteRecipesPagingSource(
                recipesApiClient = recipesApiClient,
                authRepository = authRepository,
                query = query,
            )
        }
    ).flow

    private fun createPagingConfig() = PagingConfig(
        pageSize = RecipesPageSize,
        prefetchDistance = 5,
        initialLoadSize = RecipesPageSize,
    )
}

package dev.roasti.feature.recipe.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.flow.firstOrNull
import dev.roasti.feature.auth.domain.repository.AuthRepository
import dev.roasti.feature.recipe.data.mapper.toDomain
import dev.roasti.feature.recipe.data.mapper.toQueryDto
import dev.roasti.feature.recipe.data.mapper.toRequestDto
import dev.roasti.feature.recipe.data.network.RecipesApiClient
import dev.roasti.feature.recipe.domain.model.Recipe
import dev.roasti.feature.recipe.domain.model.RecipesPagingQuery

private const val FirstPage = 1

class RemoteRecipesPagingSource(
    private val recipesApiClient: RecipesApiClient,
    private val authRepository: AuthRepository,
    private val query: RecipesPagingQuery,
) : PagingSource<Int, Recipe>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Recipe> {
        val page = params.key ?: FirstPage

        return try {
            val userId = authRepository.getUser().firstOrNull()?.id
            val response = recipesApiClient.getRecipes(
                authorId = userId,
                query = query.query.takeIf { it.isNotBlank() },
                brewMethod = query.brewMethod?.toRequestDto(),
                difficulty = query.difficulty.toQueryDto(),
                roastLevel = query.roastLevel.toQueryDto(),
                page = page,
                limit = params.loadSize,
            ).getOrThrow()

            LoadResult.Page(
                data = response.items.map { it.toDomain() },
                prevKey = if (page == FirstPage) null else page - 1,
                nextKey = response.pagination.nextPage.takeIf {
                    response.pagination.currentPage < response.pagination.lastPage
                },
            )
        } catch (error: Throwable) {
            LoadResult.Error(error)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Recipe>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val closestPage = state.closestPageToPosition(anchorPosition) ?: return null
        return closestPage.prevKey?.plus(1) ?: closestPage.nextKey?.minus(1)
    }
}

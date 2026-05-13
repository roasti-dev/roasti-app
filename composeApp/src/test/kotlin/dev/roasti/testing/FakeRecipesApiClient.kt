package dev.roasti.testing

import dev.roasti.core.network.PageResponseDto
import dev.roasti.feature.recipe.data.network.RecipesApiClient
import dev.roasti.feature.recipe.data.remote.model.BrewMethodDto
import dev.roasti.feature.recipe.data.remote.model.DifficultyDto
import dev.roasti.feature.recipe.data.remote.model.RoastLevelDto
import dev.roasti.feature.recipe.data.remote.model.request.CreateRecipeRequestDto
import dev.roasti.feature.recipe.data.remote.model.response.RecipeResponseDto

class FakeRecipesApiClient : RecipesApiClient {

    val recipeById: MutableMap<String, Result<RecipeResponseDto>> = mutableMapOf()
    val pages: MutableMap<Int, Result<PageResponseDto<RecipeResponseDto>>> = mutableMapOf()
    var getRecipeCallCount: Int = 0
        private set
    var getRecipesCallCount: Int = 0
        private set

    override suspend fun getRecipes(
        authorId: String?,
        query: String?,
        brewMethod: BrewMethodDto?,
        difficulty: DifficultyDto?,
        roastLevel: RoastLevelDto?,
        limit: Int,
        page: Int,
    ): Result<PageResponseDto<RecipeResponseDto>> {
        getRecipesCallCount++
        return pages[page] ?: Result.failure(NoSuchElementException("no fake page $page"))
    }

    override suspend fun getRecipe(id: String): Result<RecipeResponseDto> {
        getRecipeCallCount++
        return recipeById[id] ?: Result.failure(NoSuchElementException("no fake for $id"))
    }

    override suspend fun addRecipe(recipe: CreateRecipeRequestDto): Result<RecipeResponseDto> =
        error("not used in these tests")

    override suspend fun updateRecipe(
        id: String,
        recipe: CreateRecipeRequestDto,
    ): Result<RecipeResponseDto> = error("not used in these tests")

    override suspend fun removeRecipe(id: String): Result<Unit> =
        error("not used in these tests")
}

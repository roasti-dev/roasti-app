package dev.roasti.feature.recipe.domain

import kotlinx.coroutines.flow.Flow
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.Recipe
import dev.roasti.feature.recipe.domain.model.RecipeDraft
import dev.roasti.core.domain.Page
import dev.roasti.feature.recipe.domain.model.RoastLevel

interface RecipeRepository {
    suspend fun getRecipes(
        authorId: String? = null,
        query: String? = null,
        brewMethod: BrewMethod? = null,
        difficulty: Difficulty? = null,
        roastLevel: RoastLevel? = null,
        limit: Int = 50,
        page: Int = 1
    ): Result<Page<Recipe>>

    suspend fun getById(id: String): Result<Recipe>

    fun observeById(id: String): Flow<Recipe?>

    suspend fun refreshById(id: String): Result<Unit>

    suspend fun toggleLike(id: String): Result<Unit>

    suspend fun addRecipe(recipe: RecipeDraft): Result<Recipe>

    suspend fun updateRecipe(id: String, recipe: RecipeDraft): Result<Recipe>

    suspend fun removeRecipe(id: String): Result<Unit>
}

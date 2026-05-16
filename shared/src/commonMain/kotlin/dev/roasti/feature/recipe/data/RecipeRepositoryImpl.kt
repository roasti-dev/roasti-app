package dev.roasti.feature.recipe.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.likes.data.LikesApiClient
import dev.roasti.feature.recipe.data.mapper.upsertRecipe
import dev.roasti.feature.recipe.data.mapper.toDomain
import dev.roasti.feature.recipe.data.mapper.toQueryDto
import dev.roasti.feature.recipe.data.mapper.toRequestDto
import dev.roasti.feature.recipe.data.network.RecipesApiClient
import dev.roasti.feature.recipe.domain.RecipeRepository
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.Recipe
import dev.roasti.feature.recipe.domain.model.RecipeDraft
import dev.roasti.core.domain.Page
import dev.roasti.feature.recipe.domain.model.RoastLevel

// TODO: Replace Dispatchers.IO with expect/actual mechanism for KMP compatibility
//  - commonMain: expect val ioDispatcher: CoroutineDispatcher
//  - jsMain:     actual val ioDispatcher = Dispatchers.Default
//  - jvmMain:    actual val ioDispatcher = Dispatchers.IO

class RecipeRepositoryImpl(
    private val apiClient: RecipesApiClient,
    private val db: RoastiDatabaseCache,
    private val likesApiClient: LikesApiClient,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : RecipeRepository {

    override suspend fun getRecipes(
        authorId: String?,
        query: String?,
        brewMethod: BrewMethod?,
        difficulty: Difficulty?,
        roastLevel: RoastLevel?,
        limit: Int,
        page: Int
    ): Result<Page<Recipe>> {
        return apiClient.getRecipes(
            authorId = authorId,
            query = query?.takeIf { it.isNotBlank() },
            brewMethod = brewMethod?.toRequestDto(),
            difficulty = difficulty.toQueryDto(),
            roastLevel = roastLevel.toQueryDto(),
            limit = limit,
            page = page
        ).mapCatching { it.toDomain() }
    }

    override suspend fun getById(id: String): Result<Recipe> =
        apiClient.getRecipe(id)
            .mapCatching { recipe ->
                db.transaction { db.upsertRecipe(recipe) }
                recipe.toDomain()
            }
            .recoverCatching { cachedRecipeById(id) ?: throw it }

    override fun observeById(id: String): Flow<Recipe?> {
        val recipeFlow = db.recipeQueries.getRecipeById(id)
            .asFlow()
            .mapToOneOrNull(ioDispatcher)
        val stepsFlow = db.recipeStepQueries.getRecipeStepsByRecipeId(id)
            .asFlow()
            .mapToList(ioDispatcher)

        return combine(recipeFlow, stepsFlow) { recipe, steps ->
            recipe?.toDomain(steps)
        }
    }

    override suspend fun refreshById(id: String): Result<Unit> =
        apiClient.getRecipe(id).mapCatching { dto ->
            db.transaction { db.upsertRecipe(dto) }
        }

    override suspend fun toggleLike(id: String): Result<Unit> {
        val recipe = db.recipeQueries.getRecipeById(id).executeAsOneOrNull()
            ?: return Result.failure(IllegalStateException("Recipe $id is not in cache"))
        val wasLiked = recipe.is_liked == 1L

        applyLikeToggle(id, wasLiked)

        return likesApiClient.toggleLikeOnRecipe(id)
            .map { }
            .onFailure { applyLikeToggle(id, !wasLiked) }
    }

    private fun applyLikeToggle(id: String, wasLiked: Boolean) {
        db.transaction {
            db.recipeQueries.toggleLike(id)
            if (wasLiked) {
                db.recipeListMembershipQueries.deleteMembership(
                    listType = RecipeListType.FAVORITES,
                    recipeId = id,
                )
            } else {
                db.recipeListMembershipQueries.insertMembershipAtBottom(
                    listType = RecipeListType.FAVORITES,
                    recipeId = id,
                )
            }
        }
    }

    override suspend fun addRecipe(recipe: RecipeDraft): Result<Recipe> {
        return apiClient.addRecipe(recipe.toRequestDto()).mapCatching {
            db.transaction { db.upsertRecipe(it) }
            it.toDomain()
        }
    }

    override suspend fun updateRecipe(id: String, recipe: RecipeDraft): Result<Recipe> {
        return apiClient.updateRecipe(id, recipe.toRequestDto()).mapCatching {
            db.transaction { db.upsertRecipe(it) }
            it.toDomain()
        }
    }

    override suspend fun removeRecipe(id: String): Result<Unit> =
        apiClient.removeRecipe(id).onSuccess {
            db.transaction {
                db.recipeQueries.deleteRecipe(id)
                db.recipeStepQueries.deleteRecipeStepsByRecipeId(id)
            }
        }

    private fun cachedRecipeById(id: String): Recipe? {
        val cached = db.recipeQueries.getRecipeById(id).executeAsOneOrNull() ?: return null
        val steps = db.recipeStepQueries.getRecipeStepsByRecipeId(id).executeAsList()
        return cached.toDomain(steps)
    }
}

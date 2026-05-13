package dev.roasti.feature.recipe.data.mapper

import kotlinx.datetime.Instant
import dev.roasti.Recipe as CachedRecipe
import dev.roasti.RecipeStep as CachedRecipeStep
import dev.roasti.RoastiDatabaseCache
import dev.roasti.feature.recipe.data.remote.model.response.RecipeResponseDto
import dev.roasti.feature.recipe.domain.model.Author
import dev.roasti.feature.recipe.domain.model.BrewStep
import dev.roasti.feature.recipe.domain.model.Recipe
import dev.roasti.feature.recipe.domain.model.RecipeOrigin

fun RoastiDatabaseCache.upsertRecipe(recipe: RecipeResponseDto) {
    recipeQueries.insertRecipe(
        id = recipe.id,
        title = recipe.title,
        description = recipe.description,
        note = recipe.note,
        image_id = recipe.imageId,
        brew_method = recipe.brewMethod.toDomain(),
        difficulty = recipe.difficulty.toDomain(),
        roast_level = recipe.roastLevel.toDomain(),
        beans = recipe.beans,
        is_liked = if (recipe.isLiked) 1L else 0L,
        likes_count = recipe.likesCount.toLong(),
        author_id = recipe.author?.id ?: recipe.authorId,
        author_name = recipe.author?.username,
        author_image_id = recipe.author?.avatarId,
        origin_recipe_id = recipe.origin?.recipeId,
        origin_author_id = recipe.origin?.author?.id,
        origin_author_name = recipe.origin?.author?.username,
        origin_author_image_id = recipe.origin?.author?.avatarId,
        is_public = if (recipe.isPublic) 1L else 0L,
        created_at = recipe.createdAt?.toString(),
        updated_at = recipe.updatedAt?.toString(),
    )

    recipeStepQueries.deleteRecipeStepsByRecipeId(recipe.id)
    recipe.steps.orEmpty().forEach { step ->
        recipeStepQueries.insertRecipeStep(
            recipe_id = recipe.id,
            step_order = step.order.toLong(),
            title = step.title,
            description = step.description,
            duration_seconds = step.durationSeconds?.toLong(),
            image_id = step.imageId,
        )
    }
}

internal fun CachedRecipe.toDomain(steps: List<CachedRecipeStep> = emptyList()): Recipe = Recipe(
    id = id,
    title = title,
    description = description,
    note = note,
    imageId = image_id,
    brewMethod = brew_method,
    difficulty = difficulty,
    roastLevel = roast_level,
    beans = beans,
    steps = steps.map(CachedRecipeStep::toDomain),
    author = cachedAuthor(author_id, author_name, author_image_id),
    isLiked = is_liked == 1L,
    likesCount = likes_count.toInt(),
    origin = cachedOrigin(
        recipeId = origin_recipe_id,
        authorId = origin_author_id,
        authorName = origin_author_name,
        authorImageId = origin_author_image_id,
    ),
    isPublic = is_public == 1L,
    createdAt = created_at?.let { Instant.parse(it) },
    updatedAt = updated_at?.let { Instant.parse(it) },
)

private fun CachedRecipeStep.toDomain(): BrewStep = BrewStep(
    order = step_order.toInt(),
    title = title,
    description = description,
    durationSeconds = duration_seconds?.toInt(),
    imageId = image_id,
)

private fun cachedAuthor(
    id: String?,
    username: String?,
    avatarId: String?,
): Author? = if (id != null && username != null) {
    Author(
        id = id,
        username = username,
        avatarId = avatarId,
    )
} else {
    null
}

private fun cachedOrigin(
    recipeId: String?,
    authorId: String?,
    authorName: String?,
    authorImageId: String?,
): RecipeOrigin? = if (recipeId != null && authorId != null && authorName != null) {
    RecipeOrigin(
        recipeId = recipeId,
        author = Author(
            id = authorId,
            username = authorName,
            avatarId = authorImageId,
        ),
    )
} else {
    null
}

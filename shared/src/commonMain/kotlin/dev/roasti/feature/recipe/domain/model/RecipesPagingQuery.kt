package dev.roasti.feature.recipe.domain.model

data class RecipesPagingQuery(
    val query: String = "",
    val brewMethod: BrewMethod? = null,
    val difficulty: Difficulty? = null,
    val roastLevel: RoastLevel? = null,
) {
    val isDefaultFeed: Boolean
        get() = query.isBlank() &&
                brewMethod == null &&
                difficulty == null &&
                roastLevel == null
}

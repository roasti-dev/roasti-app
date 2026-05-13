package dev.roasti.ui.features.recipe.model

data class RecipeAuthorUiModel(
    val id: String,
    val username: String,
    val avatarId: String? = null,
)

data class RecipeOriginUiModel(
    val recipeId: String,
    val author: RecipeAuthorUiModel,
)

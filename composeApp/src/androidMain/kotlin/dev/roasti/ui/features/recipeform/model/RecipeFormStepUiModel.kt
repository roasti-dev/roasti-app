package dev.roasti.ui.features.recipeform.model

data class RecipeFormStepUiModel(
    val order: Int,
    val title: String,
    val description: String = "",
    val durationSeconds: Int?,
    val imageId: String? = null,
)

package dev.roasti.ui.features.recipeform.model

import java.util.UUID

data class RecipeFormStepUiModel(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val durationSeconds: Int? = null,
    val imageId: String? = null,
)

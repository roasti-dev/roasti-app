package dev.roasti.feature.recipe.domain.model

data class BrewStep(
    val order: Int,
    val title: String,
    val durationSeconds: Int?,
    val imageId: String? = null,
)

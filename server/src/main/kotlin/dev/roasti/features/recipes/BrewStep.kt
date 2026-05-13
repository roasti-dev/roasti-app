package dev.roasti.features.recipes

data class BrewStep(
    val id: Int,
    val title: String,
    val description: String?,
    val order: Int,
    val durationSeconds: Int?,
    val imageId: String?,
)

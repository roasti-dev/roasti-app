package dev.roasti.features.recipes

import dev.roasti.features.uploads.ImageId

data class BrewStep(
    val id: Int,
    val title: String,
    val description: String?,
    val order: Int,
    val durationSeconds: Int?,
    val imageId: ImageId?,
)

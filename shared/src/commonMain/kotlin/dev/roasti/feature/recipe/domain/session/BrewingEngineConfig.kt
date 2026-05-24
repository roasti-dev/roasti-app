package dev.roasti.feature.recipe.domain.session

data class BrewingEngineConfig(
    val tickIntervalMillis: Long = 50L,
    val autoAdvanceDelayMillis: Long = 1500L,
)

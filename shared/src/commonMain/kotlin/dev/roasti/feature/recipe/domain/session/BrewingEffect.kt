package dev.roasti.feature.recipe.domain.session

sealed interface BrewingEffect {
    data class StepCompleted(val stepIndex: Int) : BrewingEffect
    data class StepChanged(val fromIndex: Int, val toIndex: Int) : BrewingEffect
    data object SessionFinished : BrewingEffect
    data class AutoAdvanceArmed(val nextIndex: Int, val delayMillis: Long) : BrewingEffect
    data object AutoAdvanceCancelled : BrewingEffect
    data class AutoAdvanceFired(val toIndex: Int) : BrewingEffect
}

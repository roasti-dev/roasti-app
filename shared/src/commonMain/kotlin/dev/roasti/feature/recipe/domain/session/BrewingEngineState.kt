package dev.roasti.feature.recipe.domain.session

import dev.roasti.feature.recipe.domain.model.BrewStep

data class BrewingEngineState(
    val recipeId: String,
    val recipeTitle: String,
    val steps: List<BrewStep>,
    val currentStepIndex: Int,
    val expandedStepIndex: Int?,
    val isFinished: Boolean,
    val timer: StepTimerState,
    val autoAdvance: Boolean,
    val pendingAutoAdvance: AutoAdvanceCountdown?,
) {
    val totalSteps: Int get() = steps.size
    val isFirstStep: Boolean get() = currentStepIndex == 0
    val isLastStep: Boolean get() = currentStepIndex == totalSteps - 1
    val currentStep: BrewStep get() = steps[currentStepIndex]
    val hasTimer: Boolean get() = (currentStep.durationSeconds ?: 0) > 0
    val isTimerRunning: Boolean get() = timer.isRunning
    val stepProgress: Float get() = if (totalSteps == 0) 0f else (currentStepIndex + 1f) / totalSteps
}

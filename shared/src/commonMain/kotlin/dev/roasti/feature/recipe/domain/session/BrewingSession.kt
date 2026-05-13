package dev.roasti.feature.recipe.domain.session

import dev.roasti.feature.recipe.domain.model.BrewStep
import dev.roasti.feature.recipe.domain.model.Recipe

data class BrewingSession(
    val recipe: Recipe,
    val currentStepIndex: Int = 0,
    val isFinished: Boolean = false,
) {
    val currentStep: BrewStep get() = recipe.steps[currentStepIndex]
    val totalSteps: Int get() = recipe.steps.size
    val isFirstStep: Boolean get() = currentStepIndex == 0
    val isLastStep: Boolean get() = currentStepIndex == totalSteps - 1
    val stepProgress: Float get() = (currentStepIndex + 1f) / totalSteps
    val stepDurationSeconds: Int? get() = currentStep.durationSeconds

    fun nextStep(): BrewingSession =
        if (isLastStep) copy(isFinished = true)
        else copy(currentStepIndex = currentStepIndex + 1)

    fun previousStep(): BrewingSession =
        if (isFirstStep) this
        else copy(currentStepIndex = currentStepIndex - 1)
}

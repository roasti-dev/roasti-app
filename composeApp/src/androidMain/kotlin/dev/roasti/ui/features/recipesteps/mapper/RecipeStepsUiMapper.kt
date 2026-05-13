package dev.roasti.ui.features.recipesteps.mapper

import dev.roasti.feature.recipe.domain.model.BrewStep
import dev.roasti.feature.recipe.domain.session.BrewingSession
import dev.roasti.ui.features.recipesteps.BrewingStepUiModel
import dev.roasti.ui.features.recipesteps.SessionState
import dev.roasti.ui.features.recipesteps.StepTimerState

internal fun BrewingSession.toUiState(timer: StepTimerState) = SessionState(
    steps = recipe.steps.map(BrewStep::toUiModel),
    currentStepIndex = currentStepIndex,
    totalSteps = totalSteps,
    isFirstStep = isFirstStep,
    isLastStep = isLastStep,
    isFinished = isFinished,
    stepProgress = stepProgress,
    timer = timer,
)

private fun BrewStep.toUiModel() = BrewingStepUiModel(
    order = order,
    title = title,
    description = description,
    durationSeconds = durationSeconds,
)

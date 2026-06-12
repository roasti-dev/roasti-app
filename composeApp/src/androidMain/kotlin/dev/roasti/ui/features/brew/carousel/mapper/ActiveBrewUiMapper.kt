package dev.roasti.ui.features.brew.carousel.mapper

import dev.roasti.core.utils.imageUrl
import dev.roasti.feature.brew.domain.model.Brew
import dev.roasti.feature.brew.domain.model.BrewStatus
import dev.roasti.ui.features.brew.carousel.ActiveBrewCardUiModel
import dev.roasti.ui.features.brew.carousel.ActiveBrewProgress

internal fun Brew.toActiveCardUiModel(): ActiveBrewCardUiModel = ActiveBrewCardUiModel(
    brewId = id,
    title = recipeTitle,
    imageUrl = imageId?.let(::imageUrl),
    progress = when (status) {
        BrewStatus.WAITING -> ActiveBrewProgress.Waiting(waitUntil ?: 0L)
        else -> ActiveBrewProgress.Brewing(currentStepIndex + 1, steps.size)
    },
)

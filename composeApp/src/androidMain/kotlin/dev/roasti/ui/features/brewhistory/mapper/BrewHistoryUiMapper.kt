package dev.roasti.ui.features.brewhistory.mapper

import dev.roasti.core.datetime.formatRelative
import dev.roasti.core.utils.imageUrl
import dev.roasti.feature.brew.domain.model.Brew
import dev.roasti.ui.features.brewhistory.BrewHistoryItemUiModel
import kotlinx.datetime.Instant

internal fun Brew.toHistoryUiModel(): BrewHistoryItemUiModel = BrewHistoryItemUiModel(
    brewId = id,
    title = recipeTitle,
    imageUrl = imageId?.let(::imageUrl),
    dateLabel = finishedAt?.let { Instant.fromEpochMilliseconds(it).formatRelative() }.orEmpty(),
    note = note?.takeIf { it.isNotBlank() },
)

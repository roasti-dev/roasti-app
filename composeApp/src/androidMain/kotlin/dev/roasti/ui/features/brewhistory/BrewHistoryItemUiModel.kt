package dev.roasti.ui.features.brewhistory

internal data class BrewHistoryItemUiModel(
    val brewId: String,
    val title: String,
    val imageUrl: String?,
    val dateLabel: String,
    val note: String?,
)

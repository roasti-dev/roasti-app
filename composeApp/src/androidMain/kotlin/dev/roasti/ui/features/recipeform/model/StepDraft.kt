package dev.roasti.ui.features.recipeform.model

data class StepDraft(
    val editingIndex: Int?,
    val title: String = "",
    val durationSeconds: Int = 0,
) {
    val canConfirm: Boolean get() = title.isNotBlank()
}

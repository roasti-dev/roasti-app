package dev.roasti.ui.features.recipeform.model

data class ActiveStepSheet(
    val editingIndex: Int?,
    val title: String = "",
    val durationMinutes: String = "",
    val durationSeconds: String = "",
) {
    val canConfirm: Boolean get() = title.isNotBlank()

    val durationTotalSeconds: Int?
        get() {
            val mins = durationMinutes.toIntOrNull() ?: 0
            val secs = durationSeconds.toIntOrNull() ?: 0
            return if (mins == 0 && secs == 0) null else mins * 60 + secs
        }
}

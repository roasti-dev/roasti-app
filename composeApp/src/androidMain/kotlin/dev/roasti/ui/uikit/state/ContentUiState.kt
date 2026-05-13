package dev.roasti.ui.uikit.state

sealed interface ContentUiState<out T> {
    data object Loading : ContentUiState<Nothing>

    data class FullscreenError(val error: UiError) : ContentUiState<Nothing>

    data class Content<T>(
        val data: T,
        val isRefreshing: Boolean = false,
    ) : ContentUiState<T>
}

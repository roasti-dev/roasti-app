package dev.roasti.ui.uikit.state

sealed interface UiEvent {
    data class ShowError(val error: UiError) : UiEvent
}

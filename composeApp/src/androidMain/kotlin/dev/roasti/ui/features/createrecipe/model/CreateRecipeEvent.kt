package dev.roasti.ui.features.createrecipe.model

sealed interface CreateRecipeEvent {
    data class OnRequestFinished(val isSuccessful: Boolean) : CreateRecipeEvent
    data object OnImageUploadFailed : CreateRecipeEvent
}

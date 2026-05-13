package dev.roasti.ui.features.createrecipe

sealed class CreateRecipeScreenEvent {
    object SaveSuccess : CreateRecipeScreenEvent()
    object SaveError : CreateRecipeScreenEvent()
    object ImageUploadFailed : CreateRecipeScreenEvent()
}

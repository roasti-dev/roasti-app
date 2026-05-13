package dev.roasti.ui.features.editrecipe.model

sealed class EditRecipeEvent {
    object SaveSuccess : EditRecipeEvent()
    object SaveError : EditRecipeEvent()
    object ImageUploadFailed : EditRecipeEvent()
}

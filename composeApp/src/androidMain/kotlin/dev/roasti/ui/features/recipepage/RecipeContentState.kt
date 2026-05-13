package dev.roasti.ui.features.recipepage

sealed interface RecipeContentNavEvent {
    data object NavigateBack : RecipeContentNavEvent
}

package dev.roasti.ui.features.recipeform

import dev.roasti.ui.features.recipeform.model.RecipeFormFields

internal fun RecipeFormFields.dataEquals(other: RecipeFormFields): Boolean {
    if (title != other.title) return false
    if (description != other.description) return false
    if (imageId != other.imageId) return false
    if (brewMethod != other.brewMethod) return false
    if (difficulty != other.difficulty) return false
    if (roastLevel != other.roastLevel) return false
    if (beans != other.beans) return false
    if (steps.size != other.steps.size) return false
    return steps.zip(other.steps).all { (a, b) ->
        a.title == b.title && a.durationSeconds == b.durationSeconds && a.imageId == b.imageId
    }
}

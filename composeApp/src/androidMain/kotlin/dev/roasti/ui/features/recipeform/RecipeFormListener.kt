package dev.roasti.ui.features.recipeform

import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel

interface RecipeFormListener {
    fun onBackClick()
    fun onSaveClick()
    fun onTitleChange(value: String)
    fun onDescriptionChange(value: String)
    fun onBeansChange(value: String)
    fun onBrewMethodChange(value: BrewMethod)
    fun onDifficultyChange(value: Difficulty)
    fun onRoastLevelChange(value: RoastLevel)
    fun onUploadImage(fileName: String, bytes: ByteArray)
    fun onRemoveImage()

    fun onOpenAddStep()
    fun onOpenEditStep(index: Int)
    fun onDraftTitleChange(value: String)
    fun onDraftDurationChange(totalSeconds: Int)
    fun onCommitDraft()
    fun onCancelDraft()

    fun onRemoveStep(index: Int)
    fun onReorderSteps(fromIndex: Int, toIndex: Int)
}

package dev.roasti.ui.features.createrecipe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dev.roasti.feature.recipe.domain.RecipeRepository
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel
import dev.roasti.feature.upload.domain.UploadRepository
import dev.roasti.ui.features.recipeform.dataEquals
import dev.roasti.ui.features.recipeform.mapper.toRecipeDraft
import dev.roasti.ui.features.recipeform.model.RecipeFormFields
import dev.roasti.ui.features.recipeform.model.RecipeFormStepUiModel
import dev.roasti.ui.features.recipeform.model.StepDraft
import dev.roasti.core.utils.imageUrl

class CreateRecipeScreenViewModel(
    private val recipeRepository: RecipeRepository,
    private val uploadRepository: UploadRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(RecipeFormFields())
    val form: StateFlow<RecipeFormFields> = _form.asStateFlow()

    private val _events = MutableSharedFlow<CreateRecipeScreenEvent>()
    val events: SharedFlow<CreateRecipeScreenEvent> = _events.asSharedFlow()

    val isDirty: StateFlow<Boolean> = form
        .map { !it.dataEquals(RecipeFormFields()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun updateTitle(value: String) = _form.update { it.copy(title = value) }
    fun updateDescription(value: String) = _form.update { it.copy(description = value) }
    fun updateBrewMethod(value: BrewMethod) = _form.update { it.copy(brewMethod = value) }
    fun updateDifficulty(value: Difficulty) = _form.update { it.copy(difficulty = value) }
    fun updateRoastLevel(value: RoastLevel) = _form.update { it.copy(roastLevel = value) }
    fun updateBeans(value: String) = _form.update { it.copy(beans = value) }

    fun openAddStepEditor() = _form.update {
        it.copy(editingStep = StepDraft(editingIndex = null))
    }

    fun openEditStepEditor(index: Int) = _form.update { form ->
        val step = form.steps.getOrNull(index) ?: return@update form
        form.copy(
            editingStep = StepDraft(
                editingIndex = index,
                title = step.title,
                durationSeconds = step.durationSeconds ?: 0,
            ),
        )
    }

    fun updateDraftTitle(value: String) = _form.update {
        it.copy(editingStep = it.editingStep?.copy(title = value))
    }

    fun updateDraftDuration(totalSeconds: Int) = _form.update {
        it.copy(editingStep = it.editingStep?.copy(durationSeconds = totalSeconds))
    }

    fun commitDraft() = _form.update { form ->
        val draft = form.editingStep ?: return@update form
        if (!draft.canConfirm) return@update form
        val updatedSteps = form.steps.toMutableList()
        val idx = draft.editingIndex
        val durationOrNull = draft.durationSeconds.takeIf { it > 0 }
        if (idx == null) {
            updatedSteps.add(
                RecipeFormStepUiModel(
                    title = draft.title,
                    durationSeconds = durationOrNull,
                ),
            )
        } else {
            val existing = updatedSteps[idx]
            updatedSteps[idx] = existing.copy(
                title = draft.title,
                durationSeconds = durationOrNull,
            )
        }
        form.copy(steps = updatedSteps, editingStep = null)
    }

    fun cancelDraft() = _form.update { it.copy(editingStep = null) }

    fun removeStep(index: Int) = _form.update { form ->
        val updated = form.steps.toMutableList().also { it.removeAt(index) }
        form.copy(steps = updated)
    }

    fun reorderSteps(fromIndex: Int, toIndex: Int) = _form.update { form ->
        if (fromIndex !in form.steps.indices || toIndex !in form.steps.indices) return@update form
        val updated = form.steps.toMutableList()
        val moved = updated.removeAt(fromIndex)
        updated.add(toIndex, moved)
        form.copy(steps = updated)
    }

    fun removeImage() = _form.update { it.copy(imageId = null, imageUrl = null) }

    fun uploadImage(fileName: String, bytes: ByteArray) {
        viewModelScope.launch {
            _form.update { it.copy(isUploadingImage = true) }
            val result = uploadRepository.uploadImage(fileName, bytes)
            if (result.isFailure) _events.emit(CreateRecipeScreenEvent.ImageUploadFailed)
            _form.update { form ->
                form.copy(
                    imageId = result.getOrNull()?.id ?: form.imageId,
                    imageUrl = result.getOrNull()?.id?.let(::imageUrl) ?: form.imageUrl,
                    isUploadingImage = false,
                )
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            _form.update { it.copy(isSaving = true) }
            val result = recipeRepository.addRecipe(_form.value.toRecipeDraft())
            if (result.isSuccess) {
                _events.emit(CreateRecipeScreenEvent.SaveSuccess)
            } else {
                _form.update { it.copy(isSaving = false) }
                _events.emit(CreateRecipeScreenEvent.SaveError)
            }
        }
    }
}

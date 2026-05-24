package dev.roasti.ui.features.editrecipe

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
import dev.roasti.ui.features.editrecipe.mapper.toEditState
import dev.roasti.ui.features.editrecipe.mapper.toRecipeDraft
import dev.roasti.ui.features.editrecipe.model.EditRecipeEvent
import dev.roasti.ui.features.editrecipe.model.EditRecipeUiState
import dev.roasti.ui.features.recipeform.dataEquals
import dev.roasti.ui.features.recipeform.model.RecipeFormFields
import dev.roasti.ui.features.recipeform.model.RecipeFormStepUiModel
import dev.roasti.ui.features.recipeform.model.StepDraft
import dev.roasti.core.utils.imageUrl

class EditRecipeViewModel(
    private val recipeId: String,
    private val recipeRepository: RecipeRepository,
    private val uploadRepository: UploadRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditRecipeUiState())
    val state: StateFlow<EditRecipeUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<EditRecipeEvent>()
    val events: SharedFlow<EditRecipeEvent> = _events.asSharedFlow()

    private var initialForm: RecipeFormFields = RecipeFormFields()

    val isDirty: StateFlow<Boolean> = state
        .map { !it.form.dataEquals(initialForm) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            val recipe = recipeRepository.getById(recipeId).getOrNull()
            if (recipe != null) {
                val loaded = recipe.toEditState()
                initialForm = loaded.form
                _state.update { loaded }
            } else {
                _state.update { it.copy(isLoading = false, loadError = true) }
            }
        }
    }

    fun updateTitle(value: String) =
        _state.update { it.copyForm { copy(title = value) } }

    fun updateDescription(value: String) =
        _state.update { it.copyForm { copy(description = value) } }

    fun updateBrewMethod(value: BrewMethod) =
        _state.update { it.copyForm { copy(brewMethod = value) } }

    fun updateDifficulty(value: Difficulty) =
        _state.update { it.copyForm { copy(difficulty = value) } }

    fun updateRoastLevel(value: RoastLevel) =
        _state.update { it.copyForm { copy(roastLevel = value) } }

    fun updateBeans(value: String) = _state.update { it.copyForm { copy(beans = value) } }

    fun openAddStepEditor() = _state.update {
        it.copyForm { copy(editingStep = StepDraft(editingIndex = null)) }
    }

    fun openEditStepEditor(index: Int) = _state.update { state ->
        val step = state.form.steps.getOrNull(index) ?: return@update state
        state.copyForm {
            copy(
                editingStep = StepDraft(
                    editingIndex = index,
                    title = step.title,
                    durationSeconds = step.durationSeconds ?: 0,
                ),
            )
        }
    }

    fun updateDraftTitle(value: String) = _state.update {
        it.copyForm { copy(editingStep = editingStep?.copy(title = value)) }
    }

    fun updateDraftDuration(totalSeconds: Int) = _state.update {
        it.copyForm { copy(editingStep = editingStep?.copy(durationSeconds = totalSeconds)) }
    }

    fun commitDraft() = _state.update { state ->
        val draft = state.form.editingStep ?: return@update state
        if (!draft.canConfirm) return@update state
        state.copyForm {
            val updatedSteps = steps.toMutableList()
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
            copy(steps = updatedSteps, editingStep = null)
        }
    }

    fun cancelDraft() = _state.update { it.copyForm { copy(editingStep = null) } }

    fun removeStep(index: Int) = _state.update { state ->
        state.copyForm {
            val updated = steps.toMutableList().also { it.removeAt(index) }
            copy(steps = updated)
        }
    }

    fun reorderSteps(fromIndex: Int, toIndex: Int) = _state.update { state ->
        state.copyForm {
            if (fromIndex !in steps.indices || toIndex !in steps.indices) return@copyForm this
            val updated = steps.toMutableList()
            val moved = updated.removeAt(fromIndex)
            updated.add(toIndex, moved)
            copy(steps = updated)
        }
    }

    fun removeImage() = _state.update {
        it.copyForm { copy(imageId = null, imageUrl = null) }
    }

    fun uploadImage(fileName: String, bytes: ByteArray) {
        viewModelScope.launch {
            _state.update { it.copyForm { copy(isUploadingImage = true) } }
            val result = uploadRepository.uploadImage(fileName, bytes)
            if (result.isFailure) _events.emit(EditRecipeEvent.ImageUploadFailed)
            _state.update { state ->
                state.copyForm {
                    copy(
                        imageId = result.getOrNull()?.id ?: imageId,
                        imageUrl = result.getOrNull()?.id?.let(::imageUrl) ?: imageUrl,
                        isUploadingImage = false,
                    )
                }
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            _state.update { it.copyForm { copy(isSaving = true) } }
            val draft = state.value.toRecipeDraft()
            val result = recipeRepository.updateRecipe(recipeId, draft)
            if (result.isSuccess) {
                _events.emit(EditRecipeEvent.SaveSuccess)
            } else {
                _state.update { it.copyForm { copy(isSaving = false) } }
                _events.emit(EditRecipeEvent.SaveError)
            }
        }
    }
}

private fun EditRecipeUiState.copyForm(transform: RecipeFormFields.() -> RecipeFormFields) =
    copy(form = form.transform())

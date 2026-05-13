package dev.roasti.feature.recipe.presentation.filter

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import dev.roasti.feature.recipe.domain.model.BrewMethod
import dev.roasti.feature.recipe.domain.model.Difficulty
import dev.roasti.feature.recipe.domain.model.RoastLevel

data class RecipeFilterState(
    val brewMethod: BrewMethod? = null,
    val difficulty: Difficulty? = null,
    val roastLevel: RoastLevel? = null,
)

class RecipeFilterStore {

    private val _state: MutableStateFlow<RecipeFilterState> = MutableStateFlow(RecipeFilterState())
    val state: StateFlow<RecipeFilterState> = _state.asStateFlow()

    fun applyFilter(difficulty: Difficulty?, enabled: Boolean = true) {
        val newValue = difficulty.takeIf { enabled }
        _state.update { it.copy(difficulty = newValue) }
    }

    fun applyFilter(brewMethod: BrewMethod?, enabled: Boolean = true) {
        val newValue = brewMethod.takeIf { enabled }
        _state.update { it.copy(brewMethod = newValue) }
    }

    fun applyFilter(roastLevel: RoastLevel?, enabled: Boolean = true) {
        val newValue = roastLevel.takeIf { enabled }
        _state.update { it.copy(roastLevel = newValue) }
    }
}

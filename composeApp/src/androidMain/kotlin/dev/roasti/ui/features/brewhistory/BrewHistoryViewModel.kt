package dev.roasti.ui.features.brewhistory

import androidx.lifecycle.ViewModel
import dev.roasti.feature.brew.domain.BrewRepository
import dev.roasti.ui.features.brewhistory.mapper.toHistoryUiModel
import dev.roasti.utils.stateInWhileSubscribe
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

internal class BrewHistoryViewModel(
    brewRepository: BrewRepository,
) : ViewModel() {

    val items: StateFlow<List<BrewHistoryItemUiModel>> =
        brewRepository.observeCompleted()
            .map { brews -> brews.map { it.toHistoryUiModel() } }
            .stateInWhileSubscribe(emptyList())
}
